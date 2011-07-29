/**
 * Copyright (c) 2002-2008, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot;

import javax.swing.SwingUtilities;

/**
 * A time-consuming task to be executed asynchronously by {@link AsyncWorker}. <br />
 * Users must implement the {@link #run} method as they would do with Task. <br />
 * Exceptions and Errors thrown by the <tt>run()</tt> method will <strong>not</strong>
 * be rethrown automatically by {@link AsyncWorker#post(AsyncTask)}. <br />
 * Instead, two callbacks are provided to handle the successful run of the task or
 * its failure, respectively {@link #success(Object)} and {@link #failure(Throwable)},
 * both called in the Event Dispatch Thread when the task is finished. <br />
 * AsyncTasks cannot be reused, that is, it is not safe to pass the same instance to
 * two consecutive calls to {@link AsyncWorker#post(AsyncTask)}. <br />
 * Example:
 * <pre>
 * AsyncTask task = new AsyncTask()
 * {
 *     public Object run() throws Exception
 *     {
 *         // Called in a worker thread
 *         Thread.sleep(1000);
 *         return new ArrayList();
 *     }
 *
 *     public void success(Object result)
 *     {
 *         // Called in the Event Dispatch Thread
 *         List result = (List)result;
 *         // Do something with the List
 *     }
 *     public void failure(Throwable x)
 *     {
 *         // Report exception to the user, or just log it
 *     }
 * });
 * </pre>
 *
 * @version $Revision: 260 $
 * @see AsyncWorker
 */
public abstract class AsyncTask extends Task
{
    /**
     * Called in the Event Dispatch Thread after this AsyncTask is finished. <br />
     * Normally there is no need to override this method, as it forwards the
     * result of the task run to {@link #success(Object)} in case the task
     * ran successfully, or to {@link #failure(Throwable)} in case the task
     * did not complete successfully. <br />
     * When this method is overridden, a call to {@link #getResultOrThrow()} must
     * be done to retrieve the result of the task, or to rethrow the exception
     * thrown by the task.
     *
     * @see #success(Object)
     * @see #failure(Throwable)
     * @see #getResultOrThrow()
     */
    protected void finish()
    {
        try
        {
            success(getResultOrThrow());
        }
        catch (Throwable x)
        {
            failure(x);
        }
    }

    /**
     * Callback called in the Event Dispatch Thread in case of successful execution
     * of this AsyncTask.
     *
     * @param result The result of the task execution, as returned from {@link #run()}
     */
    public abstract void success(Object result);

    /**
     * Callback called in the Event Dispatch Thread in case of exception thrown
     * during the execution of this AsyncTask.
     *
     * @param x The Throwable thrown during the task execution.
     */
    public abstract void failure(Throwable x);

    void postRun()
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                finish();
            }
        });
    }
}
