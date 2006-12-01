/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot;

/**
 * Implementations of this interface run
 * {@link Task}s in a thread that is not the Event Dispatch Thread. <br />
 * Implementations should extend {@link AbstractWorkerThread}.
 * @version $Revision: 1.8 $
 */
public interface WorkerThread
{
   /**
    * Starts this WorkerThread, responsible for running {@link Task}s (not in the
    * Event Dispatch Thread).
    * Applets can stop threads used by implementations of this WorkerThread in any moment,
    * and this method also can be used to restart this WorkerThread
    * if it results that it is not alive anymore.
    * @see #isAlive
    */
   public void start();

   /**
    * Returns whether this WorkerThread is alive. It is not enough to return
    * whether this WorkerThread has been started, because Applets can stop threads
    * used by implementations of this WorkerThread in any moment.
    * If this WorkerThread is not alive, it must be restarted.
    * @see #start
    */
   public boolean isAlive();

   /**
    * Returns whether the current thread is a thread used by the implementation of
    * this WorkerThread to run {@link Task}s.
    */
   public boolean isWorkerThread();

   /**
    * Posts a Task to be run by this WorkerThread in a thread that is not the
    * Event Dispatch Thread. This method must be called from the
    * Event Dispatch Thread and should return immediately.
    * Implementations should check if this WorkerThread {@link #isAlive} to guarantee
    * that the posted Task will be executed by this WorkerThread.
    * @see #runTask
    */
   public void postTask(Task task);

   /**
    * Runs the given Task. This method must be called by a thread that is not the
    * Event Dispatch Thread, and must execute the task in the same thread of the
    * caller, synchronously.
    * @see #postTask
    */
   public void runTask(Task task);
}
