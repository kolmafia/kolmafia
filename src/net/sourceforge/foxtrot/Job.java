/**
 * Copyright (c) 2002-2005, Simone Bordet
 * All rights reserved.
 *
 * This software is distributable under the BSD license.
 * See the terms of the BSD license in the documentation provided with this software.
 */

package net.sourceforge.foxtrot;

/**
 * A time-consuming task to be executed in the Worker Thread that does not throw checked exceptions. <br />
 * Users must implement the {@link #run} method with the time-consuming code:
 * <pre>
 * Job task = new Job()
 * {
 *     public Object run()
 *     {
 *        long sum = 0;
 *        for (int i = 0; i < 1000000; ++i)
 *        {
 *           sum += i;
 *        }
 *        return new Integer(sum);
 *     }
 * };
 * </pre>
 * RuntimeExceptions or Errors thrown by the <code>run()</code> method will be rethrown automatically by
 * {@link Worker#post(Job)} or {@link ConcurrentWorker#post(Job)}.
 * @see Worker
 * @see ConcurrentWorker
 * @version $Revision: 1.6 $
 */
public abstract class Job extends Task
{
   /**
    * The method to implement with time-consuming code.
    * It should NOT be synchronized or synchronize on this Job instance, otherwise the AWT Event Dispatch Thread
    * cannot efficiently test when this Job is completed.
    * Overridden to remove the throws clause, so that users does not
    * have to catch unthrown exceptions.
    */
   public abstract Object run();
}
