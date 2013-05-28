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

import java.util.Collections;
import java.util.Map;

/**
 * The <b>SVNLocationEntry</b> represents a mapping of a path to its 
 * revision. That is, the repository path of an item in a particular
 * revision.  
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 * @see     ISVNLocationEntryHandler
 */
public class SVNLocationEntry {
    
    private long myRevision;
    private String myPath;
    private boolean myIsResultOfMerge;
    private Map myMergedMergeInfo;
    
    /**
     * Constructs an <b>SVNLocationEntry</b> object.
     * 
     * @param revision  a revision number
     * @param path      an item's path in the reposytory in 
     *                  the <code>revision</code>
     */
    public SVNLocationEntry(long revision, String path) {
        this(revision, path, false, null);
    }

    /**
     * Constructs an <b>SVNLocationEntry</b> object.
     * 
     * @param revision          a revision number
     * @param path              an item's path in the repository in 
     *                          the <code>revision</code>
     * @param isResultOfMerge   whether this <code>revision</code> is a result of a merge
     * @param mergedMergeInfo   merge info of this path@revision 
     * @since                   1.2.0
     */
    public SVNLocationEntry(long revision, String path, boolean isResultOfMerge, Map mergedMergeInfo) {
        myRevision = revision;
        myPath = path;
        myIsResultOfMerge = isResultOfMerge;
        myMergedMergeInfo = mergedMergeInfo != null ? Collections.unmodifiableMap(mergedMergeInfo) : null;
    }

    /**
     * Gets the path.
     * 
     * @return a path 
     */
    public String getPath() {
        return myPath;
    }
    
    /**
     * Gets the revision number.
     * 
     * @return a revision number.
     */
    public long getRevision() {
        return myRevision;
    }

    /**
     * Tells if this path@revision is a result of a merge operation.
     * 
     * <p/>
     * Note: this is always <span class="javakeyword">false</span> for location entry objects received 
     * through the public APIs. This method is not intended for API users.
     * 
     * @return  <span class="javakeyword">true</span> is it's a result of a merge; otherwise 
     *          <span class="javakeyword">false</span>
     * @since   1.2.0
     */
    public boolean isResultOfMerge() {
        return myIsResultOfMerge;
    }
    
    /**
     * Returns merge info for this path@revision.
     * 
     * <p/>
     * Note: this is always <span class="javakeyword">null</span> for location entry objects received 
     * through the public APIs. This method is not intended for API users.
     * 
     * @return merge info
     * @since  1.2.0 
     */
    public Map getMergedMergeInfo() {
        return myMergedMergeInfo;
    }
}
