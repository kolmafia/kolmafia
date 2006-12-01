/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot;

import javax.swing.SwingUtilities;

import net.sourceforge.foxtrot.workers.MultiWorkerThread;

/**
 * The class that executes {@link AsyncTask asynchronous tasks}. <br />
 * This class is used when asynchronous task processing is needed, while {@link Worker}
 * is normally used for synchronous task processing.
 * This class offers a functionality similar to what provided by
 * <a href="http://java.sun.com/products/jfc/tsc/articles/threads/update.html">
 * Sun's SwingWorker
 * </a>.
 * Example usage:
 * <pre>
 * final JButton button = new JButton("Send a message !");
 * button.addActionListener(new ActionListener()
 * {
 *    public void actionPerformed(ActionEvent e)
 *    {
 *       AsyncWorker.post(new AsyncTask()
 *       {
 *          public Object run() throws Exception
 *          {
 *             // Send some long message
 *             Thread.sleep(10000);
 *             return null;
 *          }
 *
 *          public void finish()
 *          {
 *             // Check to see if there are exceptions
 *             // by calling getResultOrThrow()
 *             try
 *             {
 *                getResultOrThrow();
 *                button.setText("Message sent !");
 *             }
 *             catch (Exception x)
 *             {
 *                // Report exception to the user, or just log it
 *             }
 *          }
 *       });
 *    }
 * });
 * </pre>
 * @version $Revision: 1.2 $
 */
public class AsyncWorker extends AbstractWorker
{
   private static AsyncWorker instance = new AsyncWorker();

   /**
    * Cannot be instantiated, use static methods only.
    */
   private AsyncWorker()
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

   /**
    * Creates and returns the default WorkerThread for this worker
    */
   WorkerThread createDefaultWorkerThread()
   {
      return new MultiWorkerThread();
   }

   /**
    * Executes asynchronously the given AsyncTask in a worker thread. <br />
    * This method returns immediately; when the AsyncTask is finished,
    * its {@link AsyncTask#finish()} method will be called in the Event Dispatch
    * Thread.
    *
    * @param task
    */
   public static void post(AsyncTask task)
   {
      instance.post(task, getWorkerThread());
   }

   private void post(AsyncTask task, WorkerThread workerThread)
   {
      boolean isEventThread = SwingUtilities.isEventDispatchThread();
      if (!isEventThread)
      {
         throw new IllegalStateException("AsyncWorker.post() can be called only from the AWT Event Dispatch Thread");
      }
      workerThread.postTask(task);
   }
}
