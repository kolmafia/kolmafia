package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.ISVNStatusFileProvider;
import org.tmatesoft.svn.core.wc.SVNEventAction;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.hooks.ISvnFileListHook;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * Represents status operation.
 * {@link #run()} method returns a set of {@link SvnStatus} objects which
 * describe the status of the working cope <code>target</code>, and its children (recursing
 * according to <code>depth</code>)..
 *
 * <p/>
 * If <code>reportAll</code> is set, retrieves all entries; otherwise,
 * retrieves only "interesting" entries (local modifications and/or out of
 * date).
 * 
 * <p/>
 * If <code>remote</code> is set, contacts the repository and augments the
 * status objects with information about out-of-date items (with respect to
 * <code>revision</code>).
 * 
 * <p/>
 * If {@link #reportExternals} is <true>true</span>, then recurses into externals
 * definitions (if any exist and <code>depth</code> is either
 * {@link SVNDepth#INFINITY} or {@link SVNDepth#UNKNOWN}) after handling the
 * main <code>target</code>. This calls the client notification handler (
 * {@link ISVNEventHandler}) with the {@link SVNEventAction#STATUS_EXTERNAL}
 * action before handling each externals definition, and with
 * {@link SVNEventAction#STATUS_COMPLETED} after each.
 * 
 * <p/>
 * <code>changeLists</code> is a collection of <code>String</code>
 * changelist names, used as a restrictive filter on items whose statuses
 * are reported; that is, doesn't report status about any item unless it's a
 * member of one of those changelists. If <code>changeLists</code> is empty
 * (or <code>null</code>), no changelist filtering
 * occurs.
 * 
 * <p/>
 * if <code>remote</code> is <code>true</span>, status is calculated against
 * this <code>revision</code>
 *
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnGetStatus extends SvnReceivingOperation<SvnStatus> {
    
    private boolean remote;
    private boolean depthAsSticky;
    private boolean reportIgnored;
    private boolean reportAll;
    private boolean reportExternals;
    private ISvnFileListHook fileListHook;
    private boolean collectParentExternals;
    private long remoteRevision;

    protected SvnGetStatus(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Returns whether to check up the status of the item in the repository, 
     * that will tell if the local item is out-of-date 
     * (like <i>'-u'</i> option in the SVN client's <code>'svn status'</code> command)
     * 
     * @return <code>true</code> if the status should be checked up in repository, otherwise <code>false</code>
     */
    public boolean isRemote() {
        return remote;
    }

    /**
     * Returns whether depth is sticky.
     * If <code>depthIsSticky</code> is set and <code>depth</code> is not
     * {@link SVNDepth#UNKNOWN}, then in addition to switching <code>target</code>'s path
     * , also sets its sticky ambient depth value to <code>depth</code>.

     * @return <code>true</code> if the depth is sticky, otherwise <code>false</code>
     */
    public boolean isDepthAsSticky() {
        return depthAsSticky;
    }

    /**
     * Returns whether to force the operation to collect information 
     * on items that were set to be ignored (like <i>'--no-ignore'</i> 
     * option in the SVN client's <code>'svn status'</code> command 
     * to disregard default and <i>'svn:ignore'</i> property ignores).
     * 
     * @return <code>true</code> if default and svn:ignore property ignores should be disregarded, otherwise <code>false</code>
     */
    public boolean isReportIgnored() {
        return reportIgnored;
    }

    /**
     * Returns whether to collect status information on all items 
     * including those ones that are in a <i>'normal'</i> state (unchanged).
     * 
     * @return <code>true</code> if all items are reported, if <code>false</code> only items with unchanged state 
     */
    public boolean isReportAll() {
        return reportAll;
    }

    /**
     * Returns whether to report externals.
     * 
     * @return <code>true</code> if externals should be reported, otherwise <code>false</code>
     */
    public boolean isReportExternals() {
        return reportExternals;
    }
    
    /**
     * Returns client's file list hook.
     * Used for 1.6 only, former {@link ISVNStatusFileProvider}.
     * 
     * @return file list hook
     */
    public ISvnFileListHook getFileListHook() {
        return this.fileListHook;
    }

    /**
     * Sets whether to check up the status of the item in the repository, 
     * that will tell if the local item is out-of-date 
     * (like <i>'-u'</i> option in the SVN client's <code>'svn status'</code> command)
     * 
     * @param remote <code>true</code> if the status should be checked up in repository, otherwise <code>false</code>
    */ 
    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    /**
     * 
     * @param depthAsSticky
     */
    public void setDepthAsSticky(boolean depthAsSticky) {
        this.depthAsSticky = depthAsSticky;
    }

    /**
     * Sets whether to force the operation to collect information 
     * on items that were set to be ignored (like <i>'--no-ignore'</i> 
     * option in the SVN client's <code>'svn status'</code> command 
     * to disregard default and <i>'svn:ignore'</i> property ignores).
     * 
     * @param reportIgnored <code>true</code> if default and svn:ignore property ignores should be disregarded, otherwise <code>false</code>
     */
    public void setReportIgnored(boolean reportIgnored) {
        this.reportIgnored = reportIgnored;
    }

    /**
     * Sets whether to collect status information on all items 
     * including those ones that are in a <i>'normal'</i> state (unchanged).
     * 
     * @param reportAll <code>true</code> if all items are reported, if <code>false</code> only items with unchanged state 
     */
    public void setReportAll(boolean reportAll) {
        this.reportAll = reportAll;
    }

    /**
     * Sets whether to report externals.
     * 
     * @param reportExternals <code>true</code> if externals should be reported, otherwise <code>false</code>
     */
    public void setReportExternals(boolean reportExternals) {
        this.reportExternals = reportExternals;
    }
    
    /**
     * Sets client's file list hook.
     * Used for 1.6 only, former {@link ISVNStatusFileProvider}.
     * 
     * @param fileListHook file list hook
     */
    public void setFileListHook(ISvnFileListHook fileListHook) {
        this.fileListHook = fileListHook;
    }
    
    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        super.ensureArgumentsAreValid();
        setRemoteRevision(SVNWCContext.INVALID_REVNUM);
        if (hasRemoteTargets()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "''{0}'' is not a local path", getFirstTarget().getURL());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
    }

    @Override
    public void initDefaults() {
        super.initDefaults();
        setRevision(SVNRevision.HEAD);
        setReportAll(true);
        setReportIgnored(true);
        setReportExternals(true);
        setRemoteRevision(SVNWCContext.INVALID_REVNUM);
    }

    /**
     * Only relevant for 1.6 working copies, obsolete (not used).
     */
    public boolean isCollectParentExternals() {
        return collectParentExternals;
    }
    
    /**
     * Only relevant for 1.6 working copies, obsolete (not used).
     */
    public void setCollectParentExternals(boolean collect) {
        this.collectParentExternals = collect;
    }

    /**
     * Sets the remove revision of the <code>target</code>.
     * 
     * @param revision remote revision
     */
    public void setRemoteRevision(long revision) {
        this.remoteRevision = revision;
    }
    
    /**
     * Returns the remove revision of the <code>target</code>.
     * This value can be accessed after operation is executed.
     * 
     * @return revision remote revision
     */
    public long getRemoteRevision() {
        return this.remoteRevision;
    }

    @Override
    public boolean isUseParentWcFormat() {
        return true;
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
