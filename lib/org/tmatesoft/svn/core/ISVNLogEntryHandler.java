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

package org.tmatesoft.svn.core;


/**
 * 
 * The <b>ISVNLogEntryHandler</b> interface should be implemented 
 * in order to handle per revision commit information (in a kind of 
 * a revisions history operation)- log entries (represented by 
 * <b>SVNLogEntry</b> objects).  
 * 
 * @version     1.3
 * @author      TMate Software Ltd.
 * @since       1.2
 * @see 		SVNLogEntry
 */
public interface ISVNLogEntryHandler {
    /**
     * 
     * Handles a log entry passed.
     * 
     * @param  logEntry 		an {@link SVNLogEntry} object 
     *                          that represents per revision information
     *                          (committed paths, log message, etc.)
     * @throws SVNException 
     */
    public void handleLogEntry(SVNLogEntry logEntry) throws SVNException;

}
