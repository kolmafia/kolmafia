/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.io;

import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLock;


/**
 * The <b>ISVNLockHandler</b> interface is used to provide some extra 
 * processing of locked/unlocked paths. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see     SVNRepository#lock(java.util.Map, String, boolean, ISVNLockHandler)
 * @see     SVNRepository#unlock(java.util.Map, boolean, ISVNLockHandler)
 */
public interface ISVNLockHandler {
    /**
     * Handles the path locked.
     * 
     * @param  path           a file path relative to the repository
     *                        root directory         
     * @param  lock           the lock set on this <code>path</code>
     * @param  error          if not <span class="javakeyword">null</code> then
     *                        it's an error message object for an error occurred 
     *                        while trying to lock an entry, in this case 
     *                        <code>lock</code> may be <span class="javakeyword">null</code> 
     * @throws SVNException
     */
    public void handleLock(String path, SVNLock lock, SVNErrorMessage error) throws SVNException;
    
    /**
     * Handles the path unlocked.
     * 
     * @param  path           a file path relative to the repository
     *                        root directory         
     * @param  lock           the lock released from this <code>path</code>
     * @param  error          if not <span class="javakeyword">null</code> then
     *                        it's an exception occurred while trying to unlock
     *                        the <code>path</code>, in this case <code>lock</code> 
     *                        may be <span class="javakeyword">null</code>
     * @throws SVNException
     */
    public void handleUnlock(String path, SVNLock lock, SVNErrorMessage error) throws SVNException;
}
