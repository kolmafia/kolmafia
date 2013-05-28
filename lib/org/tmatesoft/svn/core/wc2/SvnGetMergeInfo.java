package org.tmatesoft.svn.core.wc2;

import java.util.Map;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNMergeRange;
import org.tmatesoft.svn.core.SVNMergeRangeList;
import org.tmatesoft.svn.core.SVNURL;

/**
 * Returns mergeinfo as a <code>Map</code> with merge source URLs (as
 * {@link SVNURL}) mapped to range lists ({@link SVNMergeRangeList}). Range
 * lists are objects containing arrays of {@link SVNMergeRange ranges}
 * describing the ranges which have been merged into <code>target</code>'s URL (working copy path) as of
 * <code>target</code>'s <code>pegRevision</code>. If there is no mergeinfo, returns <code>null</code>.
 * <code>Target</code> can be either URL or working copy path.
 * 
 * <p/>
 * Note: unlike most APIs which deal with mergeinfo, this one returns data
 * where the keys of the map are absolute repository URLs rather than
 * repository filesystem paths.
 * 
 * <p/>
 * Note: this routine requires repository access.
 * 
 * {@link #run()} throws {@link org.tmatesoft.svn.core.SVNException} in the following cases:
 *             <ul>
 *             <li/>exception with {@link SVNErrorCode#UNSUPPORTED_FEATURE}
 *             error code - if the server doesn't support retrieval of
 *             mergeinfo (which will never happen for file:// URLs)
 *             </ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnGetMergeInfo extends SvnOperation<Map<SVNURL, SVNMergeRangeList>> {

    protected SvnGetMergeInfo(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Gets whether the operation changes working copy
     * @return <code>true</code> if the operation changes the working copy, otherwise <code>false</code>
     */
    @Override
    public boolean isChangesWorkingCopy() {
        return false;
    }
}
