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
package org.tmatesoft.svn.core;

import java.util.Map;

/**
 * The <code>SVNMergeInfo</code> represents information about merges to a certain repository path.
 * 
 * @version 1.3
 * @author  TMate Software Ltd.
 * @since   1.2
 */
public class SVNMergeInfo {
    private String myPath;
    private Map<String, SVNMergeRangeList> myMergeSrcPathsToRangeLists;

    /**
     * Creates a new <code>SVNMergeInfo</code> object.
     * @param path                 absolute repository path of the merge target 
     * @param srcsToRangeLists     hash that maps merge sources to merge range lists (<code>String</code> to {@link SVNMergeRangeList})
     */
    public SVNMergeInfo(String path, Map<String, SVNMergeRangeList> srcsToRangeLists) {
        myPath = path;
        myMergeSrcPathsToRangeLists = srcsToRangeLists;
    }

    /**
     * Returns the absolute repository path of the merge target.
     * @return merge target path 
     */
    public String getPath() {
        return myPath;
    }

    /**
     * Returns a hash mapping merge sources to merge range lists. 
     * Keys are <code>String</code> paths, values - {@link SVNMergeRangeList} values.
     * @return mergeinfo of the {@link #getPath() path}
     */
    public Map<String, SVNMergeRangeList> getMergeSourcesToMergeLists() {
        return myMergeSrcPathsToRangeLists;
    }

    /**
     * Returns a string representation of this object.
     * @return this object as a string
     */
    public String toString() {
		return myPath + "=" + myMergeSrcPathsToRangeLists;
	}
}
