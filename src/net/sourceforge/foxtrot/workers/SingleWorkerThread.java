/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot.workers;

import net.sourceforge.foxtrot.AbstractWorkerThread;
import net.sourceforge.foxtrot.Task;

/**
 * Full implementation of {@link foxtrot.WorkerThread} that uses a single worker thread to run
 * {@link foxtrot.Task}s subclasses. <br />
 * Tasks execution is serialized: tasks are enqueued and executed one after the other.
 * @version $Revision: 1.5 $
 */
public class SingleWorkerThread extends AbstractWorkerThread implements Runnable
{
   private static int sequence = 0;
   static final boolean debug = false;

   private Thread thread;
   private Link current;
   private boolean pending;

   public void start()
   {
      if (isAlive()) return;
      if (debug) System.out.println("[SingleWorkerThread] Starting");

      stop();

      thread = new Thread(this, getThreadName());
      // Daemon, since the JVM should shut down on Event Dispatch Thread termination
      thread.setDaemon(true);
      thread.start();
   }

   /**
    * Returns the name of the worker thread used by this WorkerThread.
    */
   protected String getThreadName()
   {
      return "Foxtrot Single Worker Thread #" + nextSequence();
   }

   static synchronized int nextSequence()
   {
      return ++sequence;
   }

   /**
    * Stops abruptly this WorkerThread.
    * If a Task is executing, its execution will be completed,
    * but pending tasks will not be executed until a restart()
    */
   protected void stop()
   {
      if (thread != null)
      {
         if (debug) System.out.println("[SingleWorkerThread] Ending " + thread);
         thread.interrupt();
      }
   }

   public boolean isAlive()
   {
      if (thread == null) return false;
      return thread.isAlive() && !isThreadInterrupted();
   }

   public boolean isWorkerThread()
   {
      return Thread.currentThread() == thread;
   }

   /**
    * Posts the given Task onto an internal queue.
    * @see #takeTask
    */
   public void postTask(Task t)
   {
      // It is possible that the worker thread is stopped when an applet is destroyed.
      // Here we restart it in case has been stopped.
      // Useful also if the WorkerThread has been replaced but not started by the user
      if (!isAlive()) start();

      // Synchronized since the variable current is accessed from two threads.
      // See takeTask()
      synchronized (this)
      {
         if (hasTasks())
         {
            if (debug) System.out.println("[SingleWorkerThread] Task queue not empty, enqueueing task:" + t);

            // Append the given task at the end of the queue
            Link item = current;
            while (item.next != null) item = item.next;
            item.next = new Link(t);
         }
         else
         {
            if (debug) System.out.println("[SingleWorkerThread] Task queue empty, adding task:" + t);

            // Add the given task and notify waiting
            current = new Link(t);
            notifyAll();
         }
      }
   }

   /**
    * Removes and returns the first available {@link foxtrot.Task} from the internal queue.
    * If no Tasks are available, this method blocks until a Task is posted via
    * {@link #postTask}
    */
   protected Task takeTask() throws InterruptedException
   {
      // Synchronized since the variable current is accessed from two threads.
      // See postTask()
      synchronized (this)
      {
         while (!hasTasks())
         {
            if (debug) System.out.println("[SingleWorkerThread] Task queue empty, waiting for tasks");
            pending = false;
            wait();
         }
         pending = true;
         // Taking the current task, removing it from the queue
         Task t = current.task;
         current = current.next;
         return t;
      }
   }

   private boolean hasTasks()
   {
      synchronized (this)
      {
         return current != null;
      }
   }

   boolean hasPendingTasks()
   {
      synchronized (this)
      {
         return pending;
      }
   }

   /**
    * Returns whether the worker thread has been interrupted or not.
    * @see java.lang.Thread#isInterrupted
    */
   protected boolean isThreadInterrupted()
   {
      return thread.isInterrupted();
   }

   /**
    * The worker thread dequeues one {@link foxtrot.Task} from the internal queue via {@link #takeTask}
    * and then executes it.
    */
   public void run()
   {
      if (debug) System.out.println("[SingleWorkerThread] Started " + thread);

      while (!isThreadInterrupted())
      {
         try
         {
            Task t = takeTask();
            if (debug) System.out.println("[SingleWorkerThread] Dequeued Task " + t);
            run(t);
         }
         catch (InterruptedException x)
         {
            if (debug) System.out.println("[SingleWorkerThread] Interrupted " + thread);
            Thread.currentThread().interrupt();
            break;
         }
      }
   }

   /**
    * Executes the given {@link foxtrot.Task}.
    * This implementation will just call {@link #runTask}.
    */
   protected void run(Task task)
   {
      runTask(task);
   }

   private static class Link
   {
      private Link next;
      private final Task task;

      private Link(Task task)
      {
         this.task = task;
      }
   }
}
