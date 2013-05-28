package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Represents info command.
 * {@link #run()} method collects information about item(s) in a repository 
 * or working copy, and returns it as {@link SvnInfo} objects.
 * 
 * <p/>
 * If <code>revision</code> and <code>target</code>'s <code>pegRevision</code>
 * are either <code>null</code> or {@link SVNRevision#isLocal() local},
 * or {@link SVNRevision#isValid() invalid}, then information will be pulled
 * solely from the working copy; no network connections will be made.
 * 
 * <p/>
 * Otherwise, information will be pulled from a repository. The actual node
 * revision selected is determined by the <code>target</code> as it exists in
 * its <code>pegRevision</code>. If <code>pegRevision</code> is
 * {@link SVNRevision#UNDEFINED}, then it defaults to
 * {@link SVNRevision#HEAD} if <code>target</code> is URL,
 * and it defaults to {@link SVNRevision#WORKING} if if <code>target</code> 
 * working copy path.
 * 
 * <p/>
 * If <code>target</code> is a file, collects its info.
 * If it is a directory, then descends according to <code>depth</code>. 
 * If <code>depth</code> is{@link SVNDepth#EMPTY}, fetches info for 
 * <code>target</code> and nothing else; if {@link SVNDepth#FILES}, for
 * <code>target</code> and its immediate file children; if
 * {@link SVNDepth#IMMEDIATES}, for the preceding plus on each immediate
 * subdirectory; if {@link SVNDepth#INFINITY}, then recurses fully, 
 * for <code>target</code> and everything beneath it.
 * 
 * <p/>
 * <code>changeLists</code> is a collection of <code>String</code>
 * changelist names, used as a restrictive filter on items whose info is
 * reported; that is, doesn't report info about any item unless it's a
 * member of one of those changelists. If <code>changeLists</code> is empty
 * (or <code>null</code>), no changelist filtering occurs.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnGetInfo extends SvnReceivingOperation<SvnInfo> {

    protected SvnGetInfo(SvnOperationFactory factory) {
        super(factory);
    }

    private boolean fetchExcluded;
    private boolean fetchActualOnly;
    
    @Override
    public void initDefaults() {
        super.initDefaults();
        setFetchActualOnly(true);
        setFetchExcluded(true);
    }

    /**
     * Sets whether to fetch excluded items.
     * 
     * @param fetchExcluded <code>true</code> if excluded items should be fetched, otherwise <code>false</code>
     */
    public void setFetchExcluded(boolean fetchExcluded) {
        this.fetchExcluded = fetchExcluded;
    }

    /**
     * Sets whether to fetch actual nodes, those are unversioned nodes that describe tree conflicts.
     * 
     * @param fetchActualOnly <code>true</code> if actual nodes should be , otherwise <code>false</code>
     */
    public void setFetchActualOnly(boolean fetchActualOnly) {
        this.fetchActualOnly = fetchActualOnly;
    }
    
    /**
     * Gets whether to fetch excluded items.
     * 
     * @return <code>true</code> if excluded items should be fetched, otherwise <code>false</code>
     */
    public boolean isFetchExcluded() {
        return fetchExcluded;
    }
    
    /**
     * Gets whether to fetch actual nodes, those are unversioned nodes that describe tree conflicts.
     * 
     * @return <code>true</code> if actual nodes should be , otherwise <code>false</code>
     */
    public boolean isFetchActualOnly() {
        return fetchActualOnly;
    }
    
    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        if (getFirstTarget().getPegRevision() == null || getFirstTarget().getPegRevision() == SVNRevision.UNDEFINED)
        {
            if (getRevision() == null || !getRevision().isValid()) {
                setRevision(hasRemoteTargets() ? SVNRevision.HEAD : SVNRevision.WORKING);
            }
        } else {
//            TODO: should we add setRevision(getFirstTarget().getPegRevision()); ? currently everything is working without this line
        }
        if (getDepth() == null || getDepth() == SVNDepth.UNKNOWN) {
            setDepth(SVNDepth.EMPTY);
        }
        
        super.ensureArgumentsAreValid();
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
