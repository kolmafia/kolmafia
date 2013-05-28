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
package org.tmatesoft.svn.core.replicator;

import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;


/**
 * The <b>ISVNReplicationHandler</b> is the interface for the handlers 
 * which are registered to a repository replicator to keep track of the 
 * replicating process. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public interface ISVNReplicationHandler {

    /**
     * Notifies this handler that the replicator is about to start a next 
     * revision copying operation. Log information taken from the source 
     * repository (from where the copy is performed) for that revision 
     * is provided.
     * 
     * @param source         the notifier        
     * @param logEntry       log info about revision changes, author, etc. 
     * @throws SVNException
     */
    public void revisionReplicating(SVNRepositoryReplicator source, SVNLogEntry logEntry) throws SVNException;

    /**
     * Notifies this handler that the replicator has just finished replicating 
     * the current revision copying operation. Commit information of the new revision 
     * committed to the destination repository is provided.
     * 
     * @param source        the notifier
     * @param commitInfo    commit information
     * @throws SVNException
     */
    public void revisionReplicated(SVNRepositoryReplicator source, SVNCommitInfo commitInfo) throws SVNException;

    /**
     * Checks if the replicating operation is cancelled. During each 
     * replicating iteration the replicator simply calls this method. A 
     * handler implementation should decide if the operation must be 
     * interrupted or not, and if for some reason it must be stopped (for example, 
     * a user said it must be cancelled), then the implementor should simply 
     * throw an <b>SVNCancelException</b>. That will terminate the operation. 
     * However it won't roll back all the previously iterations committed 
     * to the destination repository.  
     * 
     * @throws SVNCancelException   if the replicating operation is cancelled
     */
    public void checkCancelled() throws SVNCancelException;
}
