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
 * The <b>SVNAdminPath</b> is used to pass path information 
 * to <b>ISVNHistoryHandler</b> and <b>ISVNTreeHandler</b> 
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNAdminPath {
    private String myPath;
    private String myNodeID;
    private long myRevision;
    private int myTreeDepth;
    private boolean myIsDir;
    
    /**
     * Constructs a new instance of this class 
     * that is intended for {@link ISVNHistoryHandler}. 
     *  
     * @param path        an absolute repository path
     * @param nodeID      a node revision id (optional)
     * @param revision    a revision
     */
    public SVNAdminPath(String path, String nodeID, long revision) {
        myPath = path;
        myNodeID = nodeID;
        myRevision = revision;
        myTreeDepth = -1;
    }

    /**
     * Constructs a new instance of this class 
     * that is intended for {@link ISVNTreeHandler}.
     *  
     * @param path        an absolute repository path
     * @param nodeID      a node revision id (optional)
     * @param treeDepth   the depth at which <code>path</code> 
     *                    is located in the tree   
     * @param isDir       says whether <code>path</code> is 
     *                    a directory or a file
     */
    public SVNAdminPath(String path, String nodeID, int treeDepth, boolean isDir) {
        myPath = path;
        myNodeID = nodeID;
        myTreeDepth = treeDepth;
        myIsDir = isDir;
        myRevision = -1;
    }

    /**
     * Says whether <code>path</code> is 
     * a directory or a file. This information is 
     * relevant only for {@link ISVNTreeHandler}.     
     * 
     * @return <span class="javakeyword">true</span> for 
     *         a directory, <span class="javakeyword">false</span> 
     *         for a file
     */
    public boolean isDir() {
        return myIsDir;
    }

    /**
     * Returns a node revision id.
     * This information is relevant for both 
     * {@link ISVNTreeHandler} and {@link ISVNHistoryHandler}.
     * 
     * @return a node revision id
     */
    public String getNodeID() {
        return myNodeID;
    }

    /**
     * Returns an absolute path. 
     * 
     * @return an absolute path that starts with <code>'/'</code> 
     */
    public String getPath() {
        return myPath;
    }

    /**
     * Returns a revision number.  
     * This information is relevant only for 
     * {@link ISVNHistoryHandler}.
     * 
     * @return a revision number
     */
    public long getRevision() {
        return myRevision;
    }

    /**
     * Returns a tree depth for this path which is relative to the depth of the 
     * <code>SVNLookClient.doGetTree(...)</code> target path.
     * Target path which is passed to <code>SVNLookClient.doGetTree(...)</code> starts 
     * at depth 0. Then depth is incremented with every 
     * other segment of path.  
     * <p>
     * This information is relevant only for 
     * {@link ISVNTreeHandler}.
     * 
     * @return a tree depth 
     */
    public int getTreeDepth() {
        return myTreeDepth;
    }
    
    
}
