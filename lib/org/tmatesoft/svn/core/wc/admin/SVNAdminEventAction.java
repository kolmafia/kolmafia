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


/**
 * The <b>SVNAdminEventAction</b> is an enumeration of possible actions that 
 * may take place in different methods of <b>SVNAdminClient</b>. 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNAdminEventAction {
    private int myID;

    private SVNAdminEventAction(int id) {
        myID = id;
    }

    /**
     * Returns an action id
     * 
     * @return id of this action
     */
    public int getID() {
        return myID;
    }

    /**
     * Gives a string representation of this action.
     * 
     * @return string representation of this object
     */
    public String toString() {
        return Integer.toString(myID);
    }

    /**
     * An action that denotes a next revision load is started.
     */
    public static final SVNAdminEventAction REVISION_LOAD = new SVNAdminEventAction(0);
    
    /**
     * An action that denotes a next revision load is completed.
     */
    public static final SVNAdminEventAction REVISION_LOADED = new SVNAdminEventAction(1);

    /**
     * An action that denotes editing a next path within the current revision being loaded.
     */
    public static final SVNAdminEventAction REVISION_LOAD_EDIT_PATH = new SVNAdminEventAction(2);

    /**
     * An action that denotes deleting a next path within the current revision being loaded.
     */
    public static final SVNAdminEventAction REVISION_LOAD_DELETE_PATH = new SVNAdminEventAction(3);
    
    /**
     * An action that denotes adding a next path within the current revision being loaded.
     */
    public static final SVNAdminEventAction REVISION_LOAD_ADD_PATH = new SVNAdminEventAction(4);
    
    /**
     * An action that denotes replacing a next path within the current revision being loaded.
     */
    public static final SVNAdminEventAction REVISION_LOAD_REPLACE_PATH = new SVNAdminEventAction(5);
    
    /**
     * A 'next revision dumped' action.
     */
    public static final SVNAdminEventAction REVISION_DUMPED = new SVNAdminEventAction(6);

    /**
     * A 'next transaction listed' action.
     */
    public static final SVNAdminEventAction TRANSACTION_LISTED = new SVNAdminEventAction(7);

    /**
     * A 'next transaction removed' action.
     */
    public static final SVNAdminEventAction TRANSACTION_REMOVED = new SVNAdminEventAction(8);

    /**
     * Says that unlocking a path failed.
     * 
     * @since 1.2
     */
    public static final SVNAdminEventAction UNLOCK_FAILED = new SVNAdminEventAction(9);
    
    /**
     * Says that a path was successfully unlocked.
     * 
     * @since 1.2
     */
    public static final SVNAdminEventAction UNLOCKED = new SVNAdminEventAction(10);

    /**
     * Says that a path is not locked.
     * 
     * @since 1.2
     */
    public static final SVNAdminEventAction NOT_LOCKED = new SVNAdminEventAction(11);

    /**
     * A next lock is fetched from the repository.
     * 
     * @since 1.2
     */
    public static final SVNAdminEventAction LOCK_LISTED = new SVNAdminEventAction(12);

    /**
     * Says that a recovery process is about to start.
     * 
     * @since 1.2
     */
    public static final SVNAdminEventAction RECOVERY_STARTED = new SVNAdminEventAction(13);

    /**
     * Says that an upgrade process is about to start.
     * 
     * @since 1.2
     */
    public static final SVNAdminEventAction UPGRADE = new SVNAdminEventAction(14);
    
    /**
     * Says that revision properties were copied.
     *
     * @since 1.2
     */
    public static final SVNAdminEventAction REVISION_PROPERTIES_COPIED = new SVNAdminEventAction(15);

    /**
     * Says that a next revision was processed during dumpfiltering. Sent during dumpfiltering.
     * 
     * @since 1.2 
     */
    public static final SVNAdminEventAction DUMP_FILTER_REVISION_COMMITTED = new SVNAdminEventAction(16);

    /**
     * Informs that an original revision is dropped. Sent during dumpfiltering.
     * 
     * @since 1.2
     */
    public static final SVNAdminEventAction DUMP_FILTER_REVISION_SKIPPED = new SVNAdminEventAction(17);

    /**
     * Informs of the total number of dropped revisions. Sent after dumpfiltering is finished.
     * 
     * @since 1.2
     */
    public static final SVNAdminEventAction DUMP_FILTER_TOTAL_REVISIONS_DROPPED = new SVNAdminEventAction(18);

    /**
     * Informs of the original revision that was dropped during dumpfiltering. Sent only in case  
     * renumbering original revisions is enabled. Sent after dumpfiltering is finished.
     * 
     * @since 1.2
     */
    public static final SVNAdminEventAction DUMP_FILTER_DROPPED_RENUMBERED_REVISION = new SVNAdminEventAction(19);

    /**
     * Informs of an original revision that was renumbered. Sent only in case renumbering original revisions 
     * is enabled. Sent after dumpfiltering is finished.
     * 
     * @since 1.2
     */
    public static final SVNAdminEventAction DUMP_FILTER_RENUMBERED_REVISION = new SVNAdminEventAction(20);

    /**
     * Provides the total number of dropped nodes during dumpfiltering. Sent after dumpfiltering is finished.
     * 
     * @since 1.2
     */
    public static final SVNAdminEventAction DUMP_FILTER_TOTAL_NODES_DROPPED = new SVNAdminEventAction(21);

    /**
     * Informs of a dropped node during dumpfiltering. Sent after dumpfiltering is finished.
     * 
     * @since 1.2
     */
    public static final SVNAdminEventAction DUMP_FILTER_DROPPED_NODE = new SVNAdminEventAction(22);
    
    /**
     * Informs of a next shard packing start.
     * 
     * @since 1.2 
     */
    public static final SVNAdminEventAction PACK_START = new SVNAdminEventAction(23);

    /**
     * Informs of a next shard packing end.
     * 
     * @since 1.2 
     */
    public static final SVNAdminEventAction PACK_END = new SVNAdminEventAction(24);
    
    public static final SVNAdminEventAction NORMALIZED_PROPERTIES = new SVNAdminEventAction(25);

}
