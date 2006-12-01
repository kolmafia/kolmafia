/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot;

import net.sourceforge.foxtrot.workers.MultiWorkerThread;

/**
 * The class that execute time-consuming {@link Task}s and {@link Job}s, but differently
 * from {@link Worker}, execute them concurrently and not one after the other. <br />
 * Most of the times, the best choice for having synchronous behavior is {@link Worker},
 * so please think twice before using this class. <br />
 * A typical usage of this class is, for example, in an application with multiple tabs
 * where the user triggers the loading of the content of the tab by clicking on it,
 * and where tab loading is time-consuming; in such scenario, the behavior of
 * {@link Worker} would be to load the first tab and only when the first tab is loaded
 * start loading the second tab.
 * The behavior of ConcurrentWorker would be to load the tabs concurrently. <br />
 * Needless to say, this concurrent behavior imposes further care in designing the
 * system to respect thread safety, and to keep the system simple to understand. <br />
 * This approach is very similar to the asynchronous behavior, but with the advantages
 * of the synchronous model in code readability.
 */
public class ConcurrentWorker extends AbstractSyncWorker
{
   private static ConcurrentWorker instance = new ConcurrentWorker();

   /**
    * Cannot be instantiated, use static methods only.
    */
   private ConcurrentWorker()
   {
   }

   /**
    * @see Worker#getWorkerThread
    */
   public static WorkerThread getWorkerThread()
   {
      return instance.workerThread();
   }

   /**
    * @see Worker#setWorkerThread
    */
   public static void setWorkerThread(WorkerThread workerThread)
   {
      instance.workerThread(workerThread);
   }

   WorkerThread createDefaultWorkerThread()
   {
      return new MultiWorkerThread();
   }

   /**
    * @see Worker#getEventPump()
    */
   public static EventPump getEventPump()
   {
      return instance.eventPump();
   }

   /**
    * @see Worker#setEventPump(EventPump)
    */
   public static void setEventPump(EventPump eventPump)
   {
      instance.eventPump(eventPump);
   }

   /**
    * @see Worker#post(Task)
    * @see #post(Job)
    */
   public static Object post(Task task) throws Exception
   {
      return instance.post(task, getWorkerThread(), getEventPump());
   }

   /**
    * @see Worker#post(Job)
    * @see #post(Task)
    */
   public static Object post(Job job) throws Exception
   {
      return instance.post(job, getWorkerThread(), getEventPump());
   }
}
