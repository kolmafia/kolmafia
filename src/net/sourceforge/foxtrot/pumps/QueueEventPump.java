/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot.pumps;

import java.awt.AWTEvent;
import java.awt.ActiveEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.MenuComponent;
import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import net.sourceforge.foxtrot.EventPump;
import net.sourceforge.foxtrot.Task;

/**
 * Implementation of an EventPump that should work with JDK 1.2 and 1.3
 * and only uses the public API of the <code>java.awt.*</code> package.
 * @version $Revision: 1.1 $
 */
public class QueueEventPump implements EventPump, EventFilterable
{
   private static final boolean debug = false;

   private EventFilter filter;

   public void setEventFilter(EventFilter filter)
   {
      this.filter = filter;
   }

   public EventFilter getEventFilter()
   {
      return filter;
   }

   public void pumpEvents(Task task)
   {
      if (task == null) return;

      EventQueue queue = getEventQueue();

      if (debug) System.out.println("[QueueEventPump] Start pumping events - Pump is " + this + " - Task is " + task);

      while (!task.isCompleted())
      {
         try
         {
            AWTEvent event = queue.getNextEvent();

            if (debug) System.out.println("[QueueEventPump] Next Event: " + event);

            if (filter != null && !filter.accept(event))
            {
               if (debug) System.out.println("[QueueEventPump] Filtered out AWT Event: " + event + " by filter " + filter);
               continue;
            }

            try
            {
               dispatchEvent(queue, event);
            }
            catch (Throwable x)
            {
               handleThrowable(x);
            }
         }
         catch (InterruptedException x)
         {
            // AWT Thread has been interrupted, interrupt again to set again the interrupted flag
            Thread.currentThread().interrupt();
            break;
         }
      }

      if (debug) System.out.println("[QueueEventPump] Stop pumping events - Pump is " + this + " - Task is " + task);
   }

   /**
    * Copy/Paste of the Sun's JDK 1.3 implementation in java.awt.EventQueue.dispatchEvent(AWTEvent)
    * @param queue The system EventQueue
    * @param event The event to dispatch
    */
   private void dispatchEvent(EventQueue queue, AWTEvent event)
   {
      Object source = event.getSource();

      if (event instanceof ActiveEvent)
         ((ActiveEvent)event).dispatch();
      else if (source instanceof Component)
         ((Component)source).dispatchEvent(event);
      else if (source instanceof MenuComponent)
         ((MenuComponent)source).dispatchEvent(event);
      else
         System.err.println("[QueueEventPump] Unable to dispatch event " + event);
   }

   /**
    * Handle a RuntimeException or Error happened during event dispatching.
    * If the system property <code>sun.awt.exception.handler</code> is defined,
    * that handler will be used, otherwise it simply logs on <code>System.err</code>.
    */
   private void handleThrowable(Throwable x)
   {
      String handlerName = (String)AccessController.doPrivileged(new PrivilegedAction()
      {
         public Object run()
         {
            return System.getProperty("sun.awt.exception.handler");
         }
      });

      if (handlerName != null)
      {
         try
         {
            Object handler = Thread.currentThread().getContextClassLoader().loadClass(handlerName).newInstance();
            Method handle = handler.getClass().getMethod("handle", new Class[]{Throwable.class});
            handle.invoke(handler, new Object[]{x});
            return;
         }
         catch (Throwable ignored)
         {
            System.err.println("[QueueEventPump] Exception occurred while invoking AWT exception handler: " + ignored);
            // Fall through
         }
      }

      System.err.println("[QueueEventPump] Exception occurred during event dispatching:");
      x.printStackTrace();
   }

   private EventQueue getEventQueue()
   {
      return (EventQueue)AccessController.doPrivileged(new PrivilegedAction()
      {
         public Object run()
         {
            return Toolkit.getDefaultToolkit().getSystemEventQueue();
         }
      });
   }
}
