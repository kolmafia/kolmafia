/**
 * Copyright (c) 2002-2008, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot;

import net.sourceforge.foxtrot.workers.MultiWorkerThread;

/**
 * The class that execute time-consuming {@link Task}s and {@link Job}s, but differently
 * from {@link Worker}, execute them concurrently and not one after the other, though
 * the invocations to {@link #post(Task)} or {@link #post(Job)} will finish one after
 * the other. <br />
 * <br />
 * Most of the times, the best choice for having synchronous behavior is {@link Worker},
 * so please think twice before using this class. <br />
 * <br />
 * A typical usage of this class is, for example, in an application that needs to
 * communicate to a server to submit tasks and to cancel them. <br />
 * Submitting a task to the server requires an event from the user, <i>event1</i> - for
 * example clicking on a submit button, which will call <tt>ConcurrentWorker.post()</tt>
 * to submit the task <i>task1</i> to the server in <i>thread1</i>.
 * If then the user wants to cancel <i>task1</i>, it generates a new event, <i>event2</i> -
 * for example clicking on a cancel button, which will call <tt>ConcurrentWorker.post()</tt>
 * to submit a task <i>task2</i> to the server in <i>thread2</i> that will cancel
 * <i>task1</i>. <br />
 * However, since <i>event2</i> is dequeued by Foxtrot during the execution of <i>task1</i>,
 * the first invocation of <tt>ConcurrentWorker.post()</tt> (the one that submitted the
 * task to the server) does not return until <i>event2</i> and hence the second invocation
 * of <tt>ConcurrentWorker.post()</tt> is finished. <br />
 * Therefore, this class will execute tasks concurrently, but invocations of
 * <tt>ConcurrentWorker.post()</tt> are executed one after the other.
 * This ensures that the code after the second invocation of
 * <tt>ConcurrentWorker.post()</tt> is always executed before the code after the first
 * invocation of <tt>ConcurrentWorker.post()</tt> (this is not guaranteed when
 * using {@link AsyncWorker}).
 * <br />
 * Using {@link Worker} would make it impossible to cancel the task, since <i>task2</i> will
 * be executed only when <i>task1</i> is finished. <br />
 * <br />
 * Needless to say, this concurrent behavior imposes further care in designing the
 * system to respect thread safety, and to keep the system simple to understand.
 *
 * @version $Revision: 260 $
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
    public static Object post(Job job)
    {
        return instance.post(job, getWorkerThread(), getEventPump());
    }
}
