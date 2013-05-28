package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.wc.SVNRevisionRange;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * Creates a <code>SVNLogEntry</code> object with the revisions merged from
 * <code>mergeSource</code> (as of <code>mergeSource</code>'s <code>pegRevision</code>) into
 * <code>target</code> (as of <code>target</code>'s <code>pegRevision</code>).
 * <code>Target</code> can be either URL or working copy path.
 * 
 * <p/>
 * If <code>discoverChangedPaths</code> is set, then the changed paths
 * <code>Map</code> argument will be passed to a constructor of
 * {@link SVNLogEntry} on each invocation of <code>handler</code>.
 * 
 * <p/>
 * If
 * <code>revisionProperties</code> is <code>null</code>, retrieves all revision properties; 
 * else, retrieves only the revision properties named in the array (i.e. retrieves none if the array is empty).
 * 
 * Note: this operation requires repository access.
 * 
 * {@link #run()} throws {@link org.tmatesoft.svn.core.SVNException} in the following cases:
 *             <ul>
 *             <li/>exception with {@link SVNErrorCode#UNSUPPORTED_FEATURE}
 *             error code - if the server doesn't support retrieval of
 *             mergeinfo
 *             </ul>
 *             
 * @author TMate Software Ltd.
 * @version 1.7
 * @see SVNLogEntry
 */
public class SvnLogMergeInfo extends SvnReceivingOperation<SVNLogEntry> {
    
    private boolean findMerged;
    private SvnTarget source;

    private boolean discoverChangedPaths;
    private String[] revisionProperties;
    
    protected SvnLogMergeInfo(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Returns whether to report merged revisions or eligible for merge revisions
     * 
     * @return <code>true</code> if should report merged revisions, <code>false</code> if should report eligible for merge
     */
    public boolean isFindMerged() {
        return findMerged;
    }

    /**
     * Sets whether to report merged revisions or eligible for merge revisions
     * 
     * @param findMerged <code>true</code> if should report merged revisions, <code>false</code> if should report eligible for merge
     */
    public void setFindMerged(boolean findMerged) {
        this.findMerged = findMerged;
    }

    /**
     * Returns merge source, can represent URL or working copy path.
     * 
     * @return merge source
     */
    public SvnTarget getSource() {
        return source;
    }

    /**
     * Returns merge source, can represent URL or working copy path.
     * 
     * @param source merge source
     */
    public void setSource(SvnTarget source) {
        this.source = source;
    }

    /**
     * Returns whether to report of all changed paths for every revision being processed
     * If <code>true</code> then the changed paths <code>Map</code> argument will be passed to a constructor of
     * {@link SVNLogEntry}.
     * 
     * @return <code>true</code> if all changed paths for every revision being processed should be reported, otherwise <code>false</code>
     */
    public boolean isDiscoverChangedPaths() {
        return discoverChangedPaths;
    }

    /**
     * Sets whether to report of all changed paths for every revision being processed
     * If <code>true</code> then the changed paths <code>Map</code> argument will be passed to a constructor of
     * {@link SVNLogEntry}.
     * 
     * @param discoverChangedPaths <code>true</code> if all changed paths for every revision being processed should be reported, otherwise <code>false</code>
     */
    public void setDiscoverChangedPaths(boolean discoverChangedPaths) {
        this.discoverChangedPaths = discoverChangedPaths;
    }

    /**
     * Returns all revision ranges for those log should be reported.
     * 
     * @return collection of {@link SVNRevisionRange} objects
     */
    public String[] getRevisionProperties() {
        return revisionProperties;
    }

    /**
     * Sets all revision ranges for those log should be reported.
     * 
     * @param revisionProperties collection of {@link SVNRevisionRange} objects
     */
    public void setRevisionProperties(String[] revisionProperties) {
        this.revisionProperties = revisionProperties;
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        if (getDepth() == null || getDepth() == SVNDepth.UNKNOWN) {
            setDepth(SVNDepth.EMPTY);
        }
        super.ensureArgumentsAreValid();
        if (getDepth() != SVNDepth.INFINITY && getDepth() != SVNDepth.EMPTY) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.UNSUPPORTED_FEATURE, "Only depths 'infinity' and 'empty' are currently supported");
            SVNErrorManager.error(err, SVNLogType.WC);
        }
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
