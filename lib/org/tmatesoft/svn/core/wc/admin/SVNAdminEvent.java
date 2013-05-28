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
package org.tmatesoft.svn.core.wc.admin;

import java.io.File;

import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNLock;


/**
 * The <b>SVNAdminEvent</b> is a type of an event used to notify callers' handlers 
 * in several methods of <b>SVNAdminClient</b>. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNAdminEvent {
    private String myTxnName;
    private File myTxnDir;
    private long myShard;
    private long myRevision;
    private long myOriginalRevision;
    private long myDroppedRevisionsCount;
    private int myDroppedNodesCount;
    private SVNAdminEventAction myAction;
    private String myPath;
    private String myMessage; 
    private SVNLock myLock;
    private SVNErrorMessage myError;
    
    /**
     * Creates a new event.
     * 
     * @param revision               a new committed revision
     * @param originalRevision       the original revision
     * @param action                 an event action                 
     * @param message                event description message 
     */
    public SVNAdminEvent(long revision, long originalRevision, SVNAdminEventAction action, String message) {
        myRevision = revision;
        myOriginalRevision = originalRevision;
        myAction = action;
        myMessage = message;
    }

    /**
     * Creates a new event to notify about a next path being changed
     * withing the revision being currently loaded.  
     * 
     * @param action   a path change action
     * @param path     repository path being changed 
     * @param message 
     */
    public SVNAdminEvent(SVNAdminEventAction action, String path, String message) {
        myAction = action;
        myPath = path;
        if (myPath != null && myPath.startsWith("/")) {
            myPath = myPath.substring("/".length());
        }
        myMessage = message;
    }

    /**
     * Creates a new event to notify about a next shard being packed.
     * 
     * @param action  pack start\end action
     * @param shard   number of the shard being packed
     * @since 1.3
     * @see   SVNAdminClient#doPack(File)
     */
    public SVNAdminEvent(SVNAdminEventAction action, long shard) {
        myAction = action;
        myShard = shard;
    }
    
    /**
     * Creates a new event.
     * 
     * @param revision    a revision number
     * @param action      an event action
     * @param message     an event description message
     */
    public SVNAdminEvent(long revision, SVNAdminEventAction action, String message) {
        myOriginalRevision = -1;
        myRevision = -1;
        myMessage = message;
        
        if (action == SVNAdminEventAction.REVISION_LOAD) {
            myOriginalRevision = revision;    
        } else {
            myRevision = revision;            
        }
        
        myAction = action;
    }

    /**
     * Creates a new event.
     * 
     * @param txnName   a transaction name
     * @param txnDir    a transaction directory location
     * @param action    an event action
     */
    public SVNAdminEvent(String txnName, File txnDir, SVNAdminEventAction action) {
        myTxnName = txnName;
        myTxnDir = txnDir;
        myRevision = -1;
        myOriginalRevision = -1;
        myAction = action;
    }

    /**
     * Creates a new event. 
     * 
     * @param action   an event action     
     * @param lock     lock info
     * @param error    an error message (if an error occurred)
     * @param message  an event description message
     * @since          1.2.0
     */
    public SVNAdminEvent(SVNAdminEventAction action, SVNLock lock, SVNErrorMessage error, String message) {
        myError = error;
        myMessage = message;
        myAction = action;
        myLock = lock;
    }

    /**
     * Creates a new event.
     * 
     * @param action    an event action 
     * @param message   an event description message
     * @since           1.2.0
     */
    public SVNAdminEvent(SVNAdminEventAction action, String message) {
        myAction = action;
        myMessage = message;
    }

    /**
     * Creates a new event.
     * 
     * @param action   an event action 
     * @since          1.2.0
     */
    public SVNAdminEvent(SVNAdminEventAction action) {
        this(action, null);
    }

    /**
     * Returns the type of an action this event is fired for.
     * 
     * @return event action
     */
    public SVNAdminEventAction getAction() {
        return myAction;
    }
    
    /**
     * Returns an event description message.
     * If no message was provided, returns just an empty string.
     * 
     * @return   event description message
     * @since    1.2.0
     */
    public String getMessage() {
        return myMessage == null ? "" : myMessage;
    }

    /**
     * Returns the original revision from which a {@link #getRevision() new one} 
     * is loaded. 
     *  
     * @return an original revision number met in a dumpfile
     */
    public long getOriginalRevision() {
        return myOriginalRevision;
    }

    /**
     * Returns a revision.
     * 
     * <p>
     * For {@link SVNAdminClient#doDump(File, java.io.OutputStream, org.tmatesoft.svn.core.wc.SVNRevision, org.tmatesoft.svn.core.wc.SVNRevision, boolean, boolean) dump} 
     * operations it means a next dumped revision. For {@link SVNAdminClient#doLoad(File, java.io.InputStream, boolean, boolean, SVNUUIDAction, String) load} 
     * operations it means a new committed revision. 
     *   
     * @return a revision number
     */
    public long getRevision() {
        return myRevision;
    }

    /**
     * Returns a transaction directory 
     * 
     * <p>
     * Relevant for both {@link SVNAdminClient#doListTransactions(File) SVNAdminClient.doListTransactions()} 
     * and {@link SVNAdminClient#doRemoveTransactions(File, String[]) SVNAdminClient.doRemoveTransactions()} 
     * operations.
     * 
     * @return txn directory
     */
    public File getTxnDir() {
        return myTxnDir;
    }

    /**
     * Returns a transaction name.
     *
     * <p>
     * Relevant for both {@link SVNAdminClient#doListTransactions(File) SVNAdminClient.doListTransactions()} 
     * and {@link SVNAdminClient#doRemoveTransactions(File, String[]) SVNAdminClient.doRemoveTransactions()} 
     * operations.
     * 
     * @return txn name
     */
    public String getTxnName() {
        return myTxnName;
    }

    /**
     * Returns an absolute repository path being changed within 
     * the current revision load iteration.
     *  
     * @return  repository path
     */
    public String getPath() {
        return myPath;
    }

    /**
     * Returns the lock information.
     * 
     * @return     lock info
     * @since      1.2.0
     */
    public SVNLock getLock() {
        return myLock;
    }

    /**
     * Returns the error message describing the error occurred while performing an operation.
     * 
     * @return      error message
     * @since       1.2.0 
     */
    public SVNErrorMessage getError() {
        return myError;
    }

    /**
     * Returns the total number of revisions dropped during dumpfiltering.
     * 
     * @return number of dropped revisions   
     * @since   1.2.0 
     */
    public long getDroppedRevisionsCount() {
        return myDroppedRevisionsCount;
    }
    
    /**
     * Returns the total number of nodes dropped during dumpfiltering.
     * 
     * @return number of dropped nodes   
     * @since   1.2.0 
     */
    public int getDroppedNodesCount() {
        return myDroppedNodesCount;
    }

    /**
     * Sets the total number of revisions dropped during dumpfiltering.
     * 
     * <p/>
     * This method is not intended for API users. 
     * 
     * @param droppedRevisionsCount number of dropped revisions
     * @since                       1.2.0 
     */
    public void setDroppedRevisionsCount(long droppedRevisionsCount) {
        myDroppedRevisionsCount = droppedRevisionsCount;
    }

    /**
     * Sets the total number of nodes dropped during dumpfiltering.
     * 
     * <p/>
     * This method is not intended for API users. 
     * 
     * @param droppedNodesCount number of dropped nodes
     * @since                   1.2.0 
     */
    public void setDroppedNodesCount(int droppedNodesCount) {
        myDroppedNodesCount = droppedNodesCount;
    }
    
    /**
     * Returns the number of the shard packed.
     * 
     * @return shard number
     * @since  1.3
     */
    public long getShard() {
        return myShard;
    }

}
