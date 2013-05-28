/*
 * ====================================================================
 * Copyright (c) 2004-2012 TMate Software Ltd.  All rights reserved.
 *
 * This software is licensed as described in the file COPYING, which
 * you should have received as part of this distribution.  The terms
 * are also available at http://svnkit.com/license.html.
 * If newer versions of this license are posted there, you may use a
 * newer version instead, at your option.
 * ====================================================================
 */
package org.tmatesoft.svn.core.io;


/**
 * The <b>ISVNConnectionListener</b> is an interface for listeners which are invoked by {@link SVNRepository} 
 * when its connection is opened or closed.
 *  
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNConnectionListener {
    
    /**
     * Handles connection opened event. This routine is invoked by <code>repository</code> 
     * on an event when <code>repository</code> opens a new network connection. 
     * 
     * @param repository   repository object 
     */
    public void connectionOpened(SVNRepository repository);
    
    /**
     * Handles connection closed event. This routine is invoked by <code>repository</code> on an event 
     * when <code>repository</code> closes an opened network connection.
     * 
     * @param repository   repository object 
     */
    public void connectionClosed(SVNRepository repository);
}
