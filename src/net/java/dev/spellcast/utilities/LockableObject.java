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
 * <p>The problem with the <tt>synchronized</tt> keyword is that it does not allow for object
 * locks to occur beyond the death of a given thread; this means that, in order to delay the
 * processing of an object until a later time while ensuring that the object is not modified
 * in the interim, the thread must stay alive, create a new thread in order to achieve delayed
 * synchronization, or clone the object and pass the cloned object to the delayed thread.</p>
 *
 * <p>The purpose of this interface is to indicate that a given object can be locked beyond the
 * death of any particular thread by using the <i>physical</i> concept of a lock: namely,
 * something that can be unlocked using a <i>key</i>.  Note that the implementation of this
 * interface also indicates that synchronization upon an object necessarily waits for any
 * lock(s) currently in place for this object.  Implementation of this interface does not
 * require that multiple locks be supported.</p>
 */

public interface LockableObject
{
	/**
	 * Tests to see if the object is currently locked.  Note that a return
	 * of <tt>true</tt> indicates that there are <i>no</i> locks in place
	 * for this object.
	 *
	 * @return	<tt>true</tt> if this object is currently locked
	 */

	public boolean isLocked();

	/**
	 * Locks this object with the given key.  Note that in order to unlock the object,
	 * an equivalent key must be provided to the <code>unlock()</code> function.
	 * If the lock attempt is successful, this function returns true.
	 *
	 * @param	key	the key to be used to lock the object
	 * @return	<tt>true</tt> if the object was successfully locked with the given key
	 */

	public boolean lockWith( Object key );

	/**
	 * Attempts to unlock this object using the given key.  If the unlock attempt is
	 * successful or if the object is already unlocked, this function returns true.
	 *
	 * @param	testKey	the key to be used to attempt an unlock
	 * @return	<tt>true</tt> if the unlock attempt is successful
	 */

	public boolean unlockWith( Object testKey );

	/**
	 * A class used to indicate that the object has not been locked.  This is an
	 * exception that is to be used whenever a method is called that should not
	 * be called when the object is in a non-locked state.
	 */

	public class ObjectNotLockedException extends RuntimeException
	{
		public ObjectNotLockedException()
		{	super( "A locked-only method was called when the object was not in a locked state" );
		}
	}
}