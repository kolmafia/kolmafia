/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot;

import net.sourceforge.foxtrot.workers.SingleWorkerThread;

/**
 * The class that execute time-consuming {@link Task}s and {@link Job}s. <br />
 * It is normally used in event listeners that must execute time-consuming operations without
 * freezing the Swing GUI. <br />
 * Usage example (simplified from the Foxtrot examples):
 * <pre>
 * JButton button = new JButton("Take a nap!");
 * button.addActionListener(new ActionListener()
 * {
 *    public void actionPerformed(ActionEvent e)
 *    {
 *       try
 *       {
 *          Worker.post(new Task()
 *          {
 *             public Object run() throws Exception
 *             {
 *                 Thread.sleep(10000);
 *                 return null;
 *             }
 *          });
 *       }
 *       catch (Exception ignored) {}
 *    }
 * });
 * </pre>
 * While normally not necessary, it is possible to customize the two core components of this
 * class, the {@link EventPump} and the {@link WorkerThread} via the corrispondent setter
 * methods. <br />
 * This class uses by default a WorkerThread that enqueues Tasks; Tasks will be executed one
 * after the other. This is the behavior needed by most applications that want to rely on a
 * synchronous model. For an alternative behavior, see {@link ConcurrentWorker}
 * @version $Revision: 1.23 $
 */
public class Worker extends AbstractSyncWorker
{
   private static Worker instance = new Worker();

   /**
    * Cannot be instantiated, use static methods only.
    */
   private Worker()
   {
   }

   /**
    * Returns the WorkerThread used to run {@link foxtrot.Task}s subclasses in a thread
    * that is not the Event Dispatch Thread.
    * By default, the WorkerThread will enqueue the Tasks and execute them one after the
    * other.
    * @see #setWorkerThread
    */
   public static WorkerThread getWorkerThread()
   {
      return instance.workerThread();
   }

   /**
    * Sets the WorkerThread used to run {@link foxtrot.Task}s subclasses in a thread
    * that is not the Event Dispatch Thread.
    * @see #getWorkerThread
    * @throws java.lang.IllegalArgumentException If workerThread is null
    */
   public static void setWorkerThread(WorkerThread workerThread)
   {
      instance.workerThread(workerThread);
   }

   WorkerThread createDefaultWorkerThread()
   {
      return new SingleWorkerThread();
   }

   /**
    * Returns the EventPump used to pump events from the AWT Event Queue. <br />
    * If no calls to {@link #setEventPump} have been made a default pump is returned;
    * this default pump is obtained by instantiating the suitable pump for the
    * Java version that is running the code.
    * @see #setEventPump
    */
   public static EventPump getEventPump()
   {
      return instance.eventPump();
   }

   /**
    * Sets the EventPump to be used to pump events from the AWT Event Queue. <br />
    * After calling this method, subsequent invocation of {@link #post(Task)}
    * or {@link #post(Job)} will use the newly installed EventPump.
    * Use with care, since implementing correcly a new EventPump is not easy.
    * Foxtrot's default EventPumps are normally sufficient.
    * @see #getEventPump
    * @throws IllegalArgumentException If eventPump is null
    */
   public static void setEventPump(EventPump eventPump)
   {
      instance.eventPump(eventPump);
   }

   /**
    * Enqueues the given Task to be executed by the WorkerThread, while dequeueing
    * AWT events. <br />
    * If this method is called from the Event Dispatch Thread, it blocks until the task
    * has been executed, either by finishing normally or throwing an exception. <br />
    * If this method is called from a worker thread, it executes the new Task
    * immediately in the same thread and then returns the control to the calling Task. <br />
    * While Tasks are executed, AWT events are dequeued from the AWT Event Queue;
    * even in case of AWT events that throw RuntimeExceptions or Errors, this method will
    * not return until the first Task (posted from the Event Dispatch Thread) is finished.
    * @throws IllegalStateException if is not called from the Event Dispatch Thread nor
    * from a worker thread.
    * @see #post(Job job)
    */
   public static Object post(Task task) throws Exception
   {
      return instance.post(task, getWorkerThread(), getEventPump());
   }

   /**
    * Enqueues the given Job to be executed in the worker thread. <br />
    * This method behaves exactly like {@link #post(Task task)}, but it does not throw checked exceptions.
    * @throws IllegalStateException if is not called from the Event Dispatch Thread nor from another Job or Task.
    * @see #post(Task)
    */
   public static Object post(Job job)
   {
      return instance.post(job, getWorkerThread(), getEventPump());
   }
}
