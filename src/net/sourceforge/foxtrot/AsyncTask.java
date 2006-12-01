/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot;

import javax.swing.SwingUtilities;

/**
 * A time-consuming task to be executed asynchronously by {@link AsyncWorker}. <br />
 * Users must implement the {@link #run} method as they would do with Task.
 * Exceptions and Errors thrown by the <code>run()</code> method will <strong>not</strong>
 * be rethrown automatically by {@link AsyncWorker#post(AsyncTask)}.
 * Instead, {@link #getResultOrThrow} should be called, normally from inside
 * {@link #finish()}. <br />
 * The {@link #finish()} method is called in the Event Dispatch Thread when the
 * Task is finished.
 * AsyncTasks cannot be reused, that is, it is not safe to pass the same instance to
 * two consecutive calls to {@link AsyncWorker#post(AsyncTask)}.
 * Example:
 * <pre>
 * AsyncTask task = new AsyncTask()
 * {
 *    public Object run() throws Exception
 *    {
 *       // Called in a worker thread
 *       Thread.sleep(1000);
 *       return new ArrayList();
 *    }
 *
 *    public void finish()
 *    {
 *       // Called in the Event Dispatch Thread
 *       try
 *       {
 *          List result = (List)getResultOrThrow();
 *          // Do something with the List
 *       }
 *       catch (Exception x)
 *       {
 *          // Report exception to the user, or just log it
 *       }
 *    }
 * });
 * </pre>
 * @see AsyncWorker
 * @version $Revision: 1.2 $
 */
public abstract class AsyncTask extends Task
{
   /**
    * Called in the Event Dispatch Thread after this AsyncTask is finished, to
    * allow the coder to update Swing components safely, for example with the results
    * of the execution of this AsyncTask.
    * @see #getResultOrThrow()
    */
   public abstract void finish();

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
