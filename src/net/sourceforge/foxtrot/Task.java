/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot;

import java.security.AccessControlContext;
import java.security.AccessController;

import javax.swing.SwingUtilities;

/**
 * A time-consuming task to be executed in the Worker Thread that may throw checked exceptions. <br />
 * Users must implement the {@link #run} method with the time-consuming code, and not worry about
 * exceptions, for example:
 * <pre>
 * Task task = new Task()
 * {
 *     public Object run() throws InterruptedException
 *     {
 *        Thread.sleep(10000);
 *        return null;
 *     }
 * };
 * </pre>
 * Exceptions and Errors thrown by the <code>run()</code> method will be rethrown automatically by
 * {@link Worker#post(Task)} or by {@link ConcurrentWorker#post(Task)}
 * @see Worker
 * @see ConcurrentWorker
 * @version $Revision: 1.13 $
 */
public abstract class Task implements Runnable
{
   private Object result;
   private Throwable throwable;
   private boolean completed;
   private AccessControlContext securityContext;

   /**
    * Creates a new Task.
    */
   protected Task()
   {
      securityContext = AccessController.getContext();
   }

   /**
    * The method to implement with time-consuming code.
    * It should NOT be synchronized or synchronize on this Task instance, otherwise the AWT Event Dispatch Thread
    * cannot efficiently test when this Task is completed.
    */
   public abstract void run();

   /**
    * Returns the result of this Task operation, as set by {@link #setResult}.
    * If an exception or an error is thrown by {@link #run}, it is rethrown here.
    * Synchronized since the variables are accessed from 2 threads
    * Accessed from the AWT Event Dispatch Thread.
    * @see #setResult
    * @see #setThrowable
    */
   protected final synchronized Object getResultOrThrow() throws Exception
   {
      Throwable t = getThrowable();
      if (t != null)
      {
         if (t instanceof Exception)
            throw (Exception)t;
         else
            throw (Error)t;
      }
      return getResult();
   }

   /**
    * Returns the result of this Task operation, as set by {@link #setResult}.
    * Synchronized since the variable is accessed from 2 threads
    * Accessed from the AWT Event Dispatch Thread.
    * @see #getResultOrThrow
    */
   private final synchronized Object getResult()
   {
      return result;
   }

   /**
    * Sets the result of this Task operation, as returned by the {@link #run} method.
    * Synchronized since the variable is accessed from 2 threads
    * Accessed from the worker thread.
    * Package protected, used by {@link AbstractWorkerThread}
    * @see #getResultOrThrow
    * @see #getResult
    */
   final synchronized void setResult(Object result)
   {
      this.result = result;
   }

   /**
    * Returns the throwable as set by {@link #setThrowable}.
    * Synchronized since the variable is accessed from 2 threads
    * Accessed from the AWT Event Dispatch Thread.
    */
   final synchronized Throwable getThrowable()
   {
      return throwable;
   }

   /**
    * Sets the throwable eventually thrown by the {@link #run} method.
    * Synchronized since the variable is accessed from 2 threads
    * Accessed from the worker thread.
    * Package protected, used by {@link AbstractWorkerThread}
    * @see #getThrowable
    */
   final synchronized void setThrowable(Throwable x)
   {
      throwable = x;
   }

   /**
    * Returns whether the execution of this Task has been completed or not.
    */
   public final synchronized boolean isCompleted()
   {
      // Synchronized since the variable is accessed from 2 threads
      // Accessed from the AWT Event Dispatch Thread.
      return completed;
   }

   /**
    * Sets the completion status of this Task.
    * Synchronized since the variable is accessed from 2 threads.
    * Accessed from the worker thread and from the AWT Event Dispatch Thread.
    * Package protected, used by {@link AbstractWorkerThread}
    * @see #isCompleted
    */
   final synchronized void setCompleted(boolean value)
   {
      completed = value;
      if (value) notifyAll();
   }

   /**
    * Returns the protection domain stack at the moment of instantiation of this Task.
    * Synchronized since the variable is accessed from 2 threads
    * Accessed from the worker thread.
    * Package protected, used by {@link AbstractWorkerThread}
    * @see #Task
    */
   final synchronized AccessControlContext getSecurityContext()
   {
      return securityContext;
   }

   /**
    * Resets the internal status of this Task, that can be therefore be reused.
    * Synchronized since the variables are accessed from 2 threads
    * Accessed from the AWT Event Dispatch Thread.
    * Package protected, used by Worker
    * @see #isCompleted
    */
   final synchronized void reset()
   {
      setResult(null);
      setThrowable(null);
      setCompleted(false);
   }

   /**
    * Callback invoked from the worker thread to perform some operation just after the Task
    * has been {@link #isCompleted completed}.
    */
   void postRun()
   {
      // Needed in case that no events are posted on the AWT Event Queue
      // via the normal mechanisms (mouse movements, key typing, etc):
      // the AWT Event Queue is waiting in EventQueue.getNextEvent(),
      // posting this one will wake it up and allow the event pump to
      // finish its job and release control to the original pump
      SwingUtilities.invokeLater(new Runnable()
      {
         public final void run()
         {
            if (AbstractWorker.debug) System.out.println("[Task] Run completed for task " + Task.this);
         }
      });
   }
}
