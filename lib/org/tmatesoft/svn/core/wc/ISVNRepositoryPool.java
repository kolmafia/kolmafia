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
package org.tmatesoft.svn.core.wc;

import org.tmatesoft.svn.core.ISVNCanceller;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.util.ISVNDebugLog;

/**
 * The <b>ISVNRepositoryPool</b> interface is used by 
 * <b>SVN</b>*<b>Client</b> objects to create a low-level SVN protocol
 * driver that allows them to directly work with a repository.
 * 
 * <p>
 * A default implementation of the <b>ISVNRepositoryPool</b> interface - 
 * <b>DefaultSVNRepositoryPool</b> class - may cache the created
 * <b>SVNRepository</b> objects in a common pool. Several threads may
 * share that pool, but each thread is able only to retrieve those objects,
 * that belong to it (were created in that thread). 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see     DefaultSVNRepositoryPool
 */
public interface ISVNRepositoryPool {
    
    /**
     * Updates authentication manager instance referenced by {@link SVNRepository} objects 
     * currently in the pool.
     * 
     * @param authManager authentication manager instance 
     */
    public void setAuthenticationManager(ISVNAuthenticationManager authManager);
    
    /**
     * Updates canceller instance referenced by {@link SVNRepository} objects 
     * currently in the pool.
     * 
     * @param canceller      canceller instance 
     */
    public void setCanceller(ISVNCanceller canceller);

    /**
     * Updates debug log instance referenced by {@link SVNRepository} objects 
     * currently in the pool.
     * 
     * @param log debug logger object 
     */
    public void setDebugLog(ISVNDebugLog log);
    
    /**
     * Creates a low-level SVN protocol driver to access a repository.
     * 
     * <p>
     * If <code>mayReuse</code> is <span class="javakeyword">true</span>
     * and the pool feature for caching <b>SVNRepository</b> objects is supported
     * by the concrete implementation of this interface, then this method first 
     * tries to find an existing <b>SVNRepository</b> object 
     * in the pool of the current thread. If such an object is found that was
     * created for the same protocol as <code>url</code> has, then resets this 
     * object to a new <code>url</code> and returns it back. Otherwise creates
     * a new one, stores it in the thread's pool and returns back.
     * 
     * <p>
     * If <code>mayReuse</code> is <span class="javakeyword">false</span>, then
     * creates a new object, that won't be reusable.
     * 
     * @param  url            a repository location to establish a 
     *                        connection with (will be the root directory
     *                        for the working session)
     * @param  mayReuse       If <span class="javakeyword">true</span> then
     *                        retrieves/creates a reusable object, otherwise
     *                        creates a new unreusable one               
     * @return                a low-level API driver for direct interacting
     *                        with a repository
     * @throws SVNException   if <code>url</code> is malformed or there's
     *                        no appropriate implementation for a protocol
     * @see                   DefaultSVNRepositoryPool#createRepository(SVNURL, boolean)
     */
    public SVNRepository createRepository(SVNURL url, boolean mayReuse) throws SVNException;
    
    /**
     * Forces cached <b>SVNRepository</b> driver objects to close their socket 
     * connections. 
     * 
     * <p>
     * A default implementation <b>DefaultSVNRepositoryPool</b>
     * is able to cache <b>SVNRepository</b> objects in a common pool 
     * shared between multiple threads. This method allows to close
     * connections of all the cached objects.
     * 
     * @param shutdownAll if <span class="javakeyword">true</span> - closes
     *                    connections of all the <b>SVNRepository</b> objects,
     *                    if <span class="javakeyword">false</span> - connections
     *                    of only some part of <b>SVNRepository</b> objects (for example,
     *                    those, that are not needed anymore)
     * @see               DefaultSVNRepositoryPool
     * 
     * @deprecated use {@link #dispose()} method instead.
     */
    public void shutdownConnections(boolean shutdownAll);
    
    /**
     * Disposes this pool.
     * @since 1.2.0 
     */
    public void dispose();
}
