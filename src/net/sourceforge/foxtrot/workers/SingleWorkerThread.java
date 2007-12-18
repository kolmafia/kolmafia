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
 * Full implementation of {@link foxtrot.WorkerThread} that uses a single worker thread to run {@link foxtrot.Task}s
 * subclasses. <br />
 * Tasks execution is serialized: tasks are enqueued and executed one after the other.
 * 
 * @version $Revision: 1.5 $
 */
public class SingleWorkerThread
	extends AbstractWorkerThread
	implements Runnable
{
	private static int sequence = 0;
	static final boolean debug = false;

	private Thread thread;
	private Link current;
	private boolean pending;

	public void start()
	{
		if ( this.isAlive() )
		{
			return;
		}
		if ( SingleWorkerThread.debug )
		{
			System.out.println( "[SingleWorkerThread] Starting" );
		}

		this.stop();

		this.thread = new Thread( this, this.getThreadName() );
		// Daemon, since the JVM should shut down on Event Dispatch Thread termination
		this.thread.setDaemon( true );
		this.thread.start();
	}

	/**
	 * Returns the name of the worker thread used by this WorkerThread.
	 */
	protected String getThreadName()
	{
		return "Foxtrot Single Worker Thread #" + SingleWorkerThread.nextSequence();
	}

	static synchronized int nextSequence()
	{
		return ++SingleWorkerThread.sequence;
	}

	/**
	 * Stops abruptly this WorkerThread. If a Task is executing, its execution will be completed, but pending tasks will
	 * not be executed until a restart()
	 */
	protected void stop()
	{
		if ( this.thread != null )
		{
			if ( SingleWorkerThread.debug )
			{
				System.out.println( "[SingleWorkerThread] Ending " + this.thread );
			}
			this.thread.interrupt();
		}
	}

	public boolean isAlive()
	{
		if ( this.thread == null )
		{
			return false;
		}
		return this.thread.isAlive() && !this.isThreadInterrupted();
	}

	public boolean isWorkerThread()
	{
		return Thread.currentThread() == this.thread;
	}

	/**
	 * Posts the given Task onto an internal queue.
	 * 
	 * @see #takeTask
	 */
	public void postTask( final Task t )
	{
		// It is possible that the worker thread is stopped when an applet is destroyed.
		// Here we restart it in case has been stopped.
		// Useful also if the WorkerThread has been replaced but not started by the user
		if ( !this.isAlive() )
		{
			this.start();
		}

		// Synchronized since the variable current is accessed from two threads.
		// See takeTask()
		synchronized ( this )
		{
			if ( this.hasTasks() )
			{
				if ( SingleWorkerThread.debug )
				{
					System.out.println( "[SingleWorkerThread] Task queue not empty, enqueueing task:" + t );
				}

				// Append the given task at the end of the queue
				Link item = this.current;
				while ( item.next != null )
				{
					item = item.next;
				}
				item.next = new Link( t );
			}
			else
			{
				if ( SingleWorkerThread.debug )
				{
					System.out.println( "[SingleWorkerThread] Task queue empty, adding task:" + t );
				}

				// Add the given task and notify waiting
				this.current = new Link( t );
				this.notifyAll();
			}
		}
	}

	/**
	 * Removes and returns the first available {@link foxtrot.Task} from the internal queue. If no Tasks are available,
	 * this method blocks until a Task is posted via {@link #postTask}
	 */
	protected Task takeTask()
		throws InterruptedException
	{
		// Synchronized since the variable current is accessed from two threads.
		// See postTask()
		synchronized ( this )
		{
			while ( !this.hasTasks() )
			{
				if ( SingleWorkerThread.debug )
				{
					System.out.println( "[SingleWorkerThread] Task queue empty, waiting for tasks" );
				}
				this.pending = false;
				this.wait();
			}
			this.pending = true;
			// Taking the current task, removing it from the queue
			Task t = this.current.task;
			this.current = this.current.next;
			return t;
		}
	}

	private boolean hasTasks()
	{
		synchronized ( this )
		{
			return this.current != null;
		}
	}

	boolean hasPendingTasks()
	{
		synchronized ( this )
		{
			return this.pending;
		}
	}

	/**
	 * Returns whether the worker thread has been interrupted or not.
	 * 
	 * @see java.lang.Thread#isInterrupted
	 */
	protected boolean isThreadInterrupted()
	{
		return this.thread.isInterrupted();
	}

	/**
	 * The worker thread dequeues one {@link foxtrot.Task} from the internal queue via {@link #takeTask} and then
	 * executes it.
	 */
	public void run()
	{
		if ( SingleWorkerThread.debug )
		{
			System.out.println( "[SingleWorkerThread] Started " + this.thread );
		}

		while ( !this.isThreadInterrupted() )
		{
			try
			{
				Task t = this.takeTask();
				if ( SingleWorkerThread.debug )
				{
					System.out.println( "[SingleWorkerThread] Dequeued Task " + t );
				}
				this.run( t );
			}
			catch ( InterruptedException x )
			{
				if ( SingleWorkerThread.debug )
				{
					System.out.println( "[SingleWorkerThread] Interrupted " + this.thread );
				}
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	/**
	 * Executes the given {@link foxtrot.Task}. This implementation will just call {@link #runTask}.
	 */
	protected void run( final Task task )
	{
		this.runTask( task );
	}

	private static class Link
	{
		private Link next;
		private final Task task;

		private Link( final Task task )
		{
			this.task = task;
		}
	}
}
