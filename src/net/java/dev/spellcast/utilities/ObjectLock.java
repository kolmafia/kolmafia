/**
 * Copyright (c) 2003, Spellcast development team
 * http://spellcast.dev.java.net/
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  [1] Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *  [2] Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in
 *      the documentation and/or other materials provided with the
 *      distribution.
 *  [3] Neither the name "Spellcast development team" nor the names of
 *      its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written
 *      permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package net.java.dev.spellcast.utilities;

/**
 * This class is designed to be a compliment to the <code>LockableObject</code>
 * interface by providing the ability to indirectly lock any object.  Note that
 * anything implementing the <code>LockableObject</code> interface, rather than
 * reimplementing the functionality, can alternatively delegate all related
 * functionality to an <code>ObjectLock</code>.  Note that multiple locks can
 * be achieved by maintaining a list of <code>ObjectLock</code> objects and
 * testing a given key with each lock in the list.
 */

public class ObjectLock
{
	private LockSimulator simulator;
	private Object key, lockedObject;

	/**
	 * The constructor locks the object provided in the parameter, and the project
	 * will only be unlocked when the <code>unlockWith()</code> function is provided
	 * with a matching key.  Note that locking an object using <tt>null</tt> is valid,
	 * but that in this case, the object can only be unlocked using <tt>null</tt>.
	 *
	 * @param	key	the key to be used to lock this object
	 * @param	lockedObject	the object to be locked
	 */

	public ObjectLock( Object key, Object lockedObject )
	{
		this.key = key;
		this.lockedObject = lockedObject;
		lockObject();
	}

	/**
	 * Tests to see if the given key can unlock this <code>ObjectLock</code>.
	 * If this method returns <tt>true</tt> for this key, then a subsequent
	 * call to <code>unlockWith()</code> using the same key will also return
	 * <tt>true</tt> and result in a successful removal of this lock.
	 *
	 * @param	testKey	the key to be tested
	 * @return	<tt>true</tt> if the object can be unlocked with this key
	 */

	public synchronized boolean canUnlockWith( Object testKey )
	{
		if ( key == null && testKey != null )
			return false;

		if ( key != null && !key.equals( testKey ) )
			return false;

		return true;
	}

	/**
	 * Attempts to unlock this <code>ObjectLock</code> using the given key.  If
	 * the unlock attempt is successful or if the object is already unlocked,
	 * this function returns true.
	 *
	 * @param	testKey	the key to be used to attempt an unlock
	 * @return	<tt>true</tt> if the object is not locked after this function call
	 */

	public synchronized boolean unlockWith( Object testKey )
	{
		if ( !canUnlockWith( testKey ) )
			return false;

		releaseLock();
		return true;
	}

	/**
	 * Attempts to relock this <code>ObjectLock</code> using the original key.
	 * If the lock has not yet been unlocked, this function returns false.
	 *
	 * @return	<tt>false</tt> if the lock has not yet been unlocked
	 */

	public synchronized boolean relock()
	{
		if ( isLocked() )
			return false;
		lockObject();
		return true;
	}

	/**
	 * Tests to see if the object is currently locked using this <code>ObjectLock</code>.
	 * Note that this will not detect if the object is currently locked through another
	 * means.
	 *
	 * @return	<tt>true</tt> if this object lock is attempting to lock this object
	 */

	public synchronized boolean isLocked()
	{	return simulator != null && simulator.isAlive();
	}

	/**
	 * An internal module used to place a lock on the given object.  This method creates
	 * a new lock simulation thread if one is not already in place, and does nothing in
	 * the event that this <code>ObjectLock</code> has already locked the object.
	 */

	private synchronized void lockObject()
	{
		if ( isLocked() )
			return;
		simulator = new LockSimulator();
		simulator.simulateLock();
	}

	/**
	 * An internal module used to release the lock on the given object.  This method
	 * merely interrupts the lock simulation thread and nulls the reference in order
	 * to assist the garbage collector.
	 */

	private synchronized void releaseLock()
	{
		if ( !isLocked() )
			return;
		simulator.simulateUnlock();
		simulator = null;
	}

	/**
	 * An internal class used to achieve the indefinite locking state.  The thread
	 * merely puts itself to sleep for the maximum amount of time possible, repeatedly,
	 * until the thread is unlocked by a call to <code>simulateUnlock</code>.  Note
	 * that this thread is a daemon thread for convenience.
	 */

	private class LockSimulator extends Thread
	{
		/**
		 * Constructs a new <code>LockSimulator</code>; note this is achieved simply
		 * by calling the default <code>Thread</code> constructor and setting the
		 * thread as a daemon thread so that it does not disable the program from
		 * closing down cleanly.
		 */

		public LockSimulator()
		{	setDaemon( true );
		}

		/**
		 * This method simulates the actual object locking.  The locking is achieved
		 * by starting this thread, which runs the appropriate loop.
		 */

		public void simulateLock()
		{	start();
		}

		/**
		 * This method simulates the actual object unlocking.  The unlocking is achieved
		 * by interrupting the thread being put to sleep.  Note that though this kills
		 * the thread, it does not immediately result in the call to <code>isLocked()</code>
		 * returning <tt>false</tt>, since the <code>InterruptedException</code> needs
		 * to propogate before that may happen.
		 */

		public void simulateUnlock()
		{	interrupt();
		}

		/**
		 * Locks the object by synchronizing on the object until the <code>ObjectLock</code>
		 * is told that the lock should be released.  This is achieved through a loop where
		 * <code>Long.MAX_VALUE</code> is used as the sleep increment; though this, in practice,
		 * would in of itself be a permanent lock, the <code>while ( isLocked() )</code> loop
		 * is used to be more proper.
		 */

		public void run()
		{
			synchronized ( lockedObject )
			{
				try
				{
					while ( isLocked() )
						sleep( Long.MAX_VALUE );
				}
				catch ( InterruptedException e )
				{
				}
			}
		}
	}
}