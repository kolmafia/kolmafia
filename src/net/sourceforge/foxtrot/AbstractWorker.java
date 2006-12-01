/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot;

/**
 * Base class for all Foxtrot workers, both synchronous and asynchronous.
 * @version $Revision: 1.2 $
 */
abstract class AbstractWorker
{
   static final boolean debug = false;

   private WorkerThread workerThread;

   AbstractWorker()
   {
   }

   /**
    * Returns the WorkerThread used to run {@link foxtrot.Task}s subclasses in a thread
    * that is not the Event Dispatch Thread. <br />
    * Uses a C-style getter method to avoid clash with the static getter method
    * present in subclasses for API compatibility.
    * @see #workerThread(WorkerThread)
    */
   WorkerThread workerThread()
   {
      if (workerThread == null) workerThread(createDefaultWorkerThread());
      return workerThread;
   }

   /**
    * Sets the WorkerThread used to run {@link foxtrot.Task}s subclasses in a thread
    * that is not the Event Dispatch Thread.
    * Uses a C-style setter method to avoid clash with the static setter method
    * present in subclasses for API compatibility.
    * @see #workerThread()
    * @throws java.lang.IllegalArgumentException If workerThread is null
    */
   void workerThread(WorkerThread workerThread)
   {
      if (workerThread == null) throw new IllegalArgumentException("WorkerThread cannot be null");
      this.workerThread = workerThread;
      if (debug) System.out.println("[AbstractWorker] Initialized WorkerThread: " + workerThread);
   }

   /**
    * Creates a default WorkerThread instance for this worker.
    */
   abstract WorkerThread createDefaultWorkerThread();
}
