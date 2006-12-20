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
import java.awt.Toolkit;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

import net.sourceforge.foxtrot.EventPump;
import net.sourceforge.foxtrot.Task;

/**
 * This implementation of EventPump calls the package protected method
 * <code>java.awt.EventDispatchThread.pumpEvents(Conditional)</code>
 * to pump events while a Task is executed.
 * @version $Revision: 1.1 $
 */
public class ConditionalEventPump implements EventPump, EventFilterable
{
   private static Class conditionalClass;
   private static Method pumpEventsMethod;
   static final boolean debug = false;

   static
   {
      try
      {
         AccessController.doPrivileged(new PrivilegedExceptionAction()
         {
            public Object run() throws ClassNotFoundException, NoSuchMethodException
            {
               ClassLoader loader = ClassLoader.getSystemClassLoader();
               conditionalClass = loader.loadClass("java.awt.Conditional");
               Class dispatchThreadClass = loader.loadClass("java.awt.EventDispatchThread");
               pumpEventsMethod = dispatchThreadClass.getDeclaredMethod("pumpEvents", new Class[]{conditionalClass});
               pumpEventsMethod.setAccessible(true);

               // See remarks for use of this property in java.awt.EventDispatchThread
               String property = "sun.awt.exception.handler";
               String handler = System.getProperty(property);
               if (handler == null)
               {
                  handler = ThrowableHandler.class.getName();
                  System.setProperty(property, handler);
                  if (debug) System.out.println("[ConditionalEventPump] Installing AWT Throwable Handler " + handler);
               }
               else
               {
                  if (debug) System.out.println("[ConditionalEventPump] Using already installed AWT Throwable Handler " + handler);
               }
               return null;
            }
         });
      }
      catch (Throwable x)
      {
         if (debug) x.printStackTrace();
         throw new Error(x.toString());
      }
   }

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
      // A null task may be passed for initialization of this class.
      if (task == null) return;

      try
      {
         if (debug) System.out.println("[ConditionalEventPump] Start pumping events - Pump is " + this + " - Task is " + task);

         // Invoke java.awt.EventDispatchThread.pumpEvents(new Conditional(task));
         Object conditional = Proxy.newProxyInstance(conditionalClass.getClassLoader(), new Class[]{conditionalClass}, new Conditional(task));
         pumpEventsMethod.invoke(Thread.currentThread(), new Object[]{conditional});
      }
      catch (InvocationTargetException x)
      {
         // No exceptions should escape from java.awt.EventDispatchThread.pumpEvents(Conditional)
         // since we installed a throwable handler. But one provided by the user may fail.
         Throwable t = x.getTargetException();
         System.err.println("[ConditionalEventPump] Exception occurred during event dispatching:");
         t.printStackTrace();

         // Rethrow. This will exit from Worker.post with a runtime exception or an error, and
         // the original event pump will take care of it.
         if (t instanceof RuntimeException)
            throw (RuntimeException)t;
         else
            throw (Error)t;
      }
      catch (Throwable x)
      {
         // Here we have an compiler bug
         System.err.println("[ConditionalEventPump] PANIC: uncaught exception in Foxtrot code");
         x.printStackTrace();
      }
      finally
      {
         // We're not done. Because of bug #4531693 (see Conditional) pumpEvents() may have returned
         // immediately, but the Task is not completed. Same may happen in case of buggy exception handler.
         // Here wait until the Task is completed. Side effect is freeze of the GUI.
         waitForTask(task);

         if (debug) System.out.println("[ConditionalEventPump] Stop pumping events - Pump is " + this + " - Task is " + task);
      }
   }

   private void waitForTask(Task task)
   {
      try
      {
         synchronized (task)
         {
            while (!task.isCompleted())
            {
               if (debug) System.out.println("[ConditionalEventPump] Waiting for Task " + task + " to complete (GUI freeze)");
               task.wait();
            }
            if (debug) System.out.println("[ConditionalEventPump] Task " + task + " is completed");
         }
      }
      catch (InterruptedException x)
      {
         // Someone interrupted the Event Dispatch Thread, re-interrupt
         Thread.currentThread().interrupt();
      }
   }

   /**
    * This method is called before an event is got from the EventQueue and dispatched,
    * to see if pumping of events should continue or not.
    * Returns true to indicate that pumping should continue, false to indicate that pumping
    * should stop.
    */
   private Boolean pumpEvent(Task task)
   {
      Boolean completed = task.isCompleted() ? Boolean.TRUE : Boolean.FALSE;
      // Task already completed, return false to indicate to stop pumping events
      if (completed.booleanValue()) return Boolean.FALSE;

      while (true)
      {
         // The task is still running, we should pump events
         AWTEvent nextEvent = waitForEvent();
         if (nextEvent == null) return Boolean.FALSE;
         if (debug) System.out.println("[ConditionalEventPump] Next Event: " + nextEvent);

         // If this event cannot be pumped, we interrupt immediately event pumping
         // The GUI will freeze, but we still wait for the Task to finish (see pumpEvents())
         if (!canPumpEvent(nextEvent)) return Boolean.FALSE;

         // Plug the event filtering mechanism
         if (filter == null || filter.accept(nextEvent)) return Boolean.TRUE;

         // The event has been filtered out, pop it from the EventQueue
         // then wait again for the next event
         nextEvent = getNextEvent();
         if (nextEvent == null) return Boolean.FALSE;
         if (debug) System.out.println("[ConditionalEventPump] Filtered out AWT Event: " + nextEvent + " by filter " + filter);
      }
   }

   /**
    * Returns whether this event can be pumped from the EventQueue.
    * JDK 1.4 introduced SequencedEvent, which is an event holding a list SequencedEvents
    * that should be dispatched in order.
    * Bug #4531693 was caused by the fact that the first SequencedEvent of a list, when
    * dispatched, might end up calling an event listener that displayed a dialog (or called Foxtrot,
    * that uses the same event pumping mechanism); the new event pump might try to dispatch the
    * SequencedEvent second in the list (while the first wasn't completely dispatched yet),
    * causing the application to hang.
    * Bug #4531693 has been fixed in JDK 1.4.2, and backported to 1.4.1, so there is no longer
    * need to check for this situation, unless using JDK 1.4.0 or non-fixed versions of JDK 1.4.1.
    */
   protected boolean canPumpEvent(AWTEvent event)
   {
      return true;
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

   private AWTEvent getNextEvent()
   {
      try
      {
         return getEventQueue().getNextEvent();
      }
      catch (InterruptedException x)
      {
         Thread.currentThread().interrupt();
         return null;
      }
   }

   /**
    * Waits until an event is available on the EventQueue.
    * This method uses the same synchronization mechanisms used by EventQueue to be notified when
    * an event is posted on the EventQueue.
    * Waiting for events is necessary in this case: a Task is posted and we would like to start
    * pumping, but no events have been posted yet ({@link #peekEvent} returns null).
    */
   private AWTEvent waitForEvent()
   {
      EventQueue queue = getEventQueue();
      AWTEvent nextEvent = null;
      synchronized (queue)
      {
         while ((nextEvent = peekEvent(queue)) == null)
         {
            if (debug) System.out.println("[ConditionalEventPump] Waiting for events...");
            try
            {
               queue.wait();
            }
            catch (InterruptedException x)
            {
               Thread.currentThread().interrupt();
               return null;
            }
         }
      }
      return nextEvent;
   }

   /**
    * Peeks the EventQueue for the next event, without removing it.
    */
   protected AWTEvent peekEvent(EventQueue queue)
   {
      return queue.peekEvent();
   }

   /**
    * Implements the <code>java.awt.Conditional</code> interface,
    * that is package private, with a JDK 1.3+ dynamic proxy.
    */
   private class Conditional implements InvocationHandler
   {
      private final Task task;

      /**
       * Creates a new invocation handler for the given task.
       */
      private Conditional(Task task)
      {
         this.task = task;
      }

      /**
       * Implements method <code>java.awt.Conditional.evaluate()</code>
       */
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
      {
         return pumpEvent(task);
      }
   }

   /**
    * Handler for RuntimeExceptions or Errors thrown during dispatching of AWT events. <br />
    * The name of this class is used as a value of the property <code>sun.awt.exception.handler</code>,
    * and the AWT event dispatch mechanism calls it when an unexpected runtime exception or error
    * is thrown during event dispatching. If the user specifies a different exception handler,
    * this one will not be used, and the user's one is used instead.
    * Use of this class is necessary in JDK 1.4, since RuntimeExceptions and Errors are propagated to
    * be handled by the ThreadGroup (but not for modal dialogs).
    */
   public static class ThrowableHandler
   {
      /**
       * The callback method invoked by the AWT event dispatch mechanism when an unexpected
       * exception or error is thrown during event dispatching. <br>
       * It just logs the exception.
       */
      public void handle(Throwable t)
      {
         System.err.println("[ConditionalEventPump] Exception occurred during event dispatching:");
         t.printStackTrace();
      }
   }
}
