package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;

/**
* Represents commit operation. Commits files or directories into repository.
* 
* <p/>
* If <code>targets</code> has zero elements, then do nothing and return
* immediately without error.
* 
* <p/>
* If the caller's {@link ISVNEventHandler event handler} is not 
* <code>null</code> it will be called as the commit
* progresses with any of the following actions:
* {@link SVNEventAction#COMMIT_MODIFIED},
* {@link SVNEventAction#COMMIT_ADDED},
* {@link SVNEventAction#COMMIT_DELETED},
* {@link SVNEventAction#COMMIT_REPLACED}. If the commit succeeds, the
* handler will be called with {@link SVNEventAction#COMMIT_COMPLETED} event
* action.
* 
* <p/>
* If <code>depth</code> is {@link SVNDepth#INFINITY}, commits all changes
* to and below named targets. If <code>depth</code> is
* {@link SVNDepth#EMPTY}, commits only named targets (that is, only
* property changes on named directory targets, and property and content
* changes for named file targets). If <code>depth</code> is
* {@link SVNDepth#FILES}, behaves as above for named file targets, and for
* named directory targets, commits property changes on a named directory
* and all changes to files directly inside that directory. If
* {@link SVNDepth#IMMEDIATES}, behaves as for {@link SVNDepth#FILES}, and
* for subdirectories of any named directory <code>target</code> commits as though for
* {@link SVNDepth#EMPTY}.
* 
* <p/>
* Unlocks paths in the repository, unless <code>keepLocks</code> is <code>true</code>.
* 
* <p/>
* <code>changelists</code> used as a restrictive filter on items that are committed; that is,
* doesn't commit anything unless it's a member of one of those changelists.
* After the commit completes successfully, removes changelist associations
* from the targets, unless <code>keepChangelist</code> is set. If
* <code>changelists</code> is empty (or altogether <code>null</code>), no changelist filtering occurs.
* 
* <p/>
* If no exception is thrown and {@link SVNCommitInfo#getNewRevision()} is
* invalid (<code>&lt;0</code>), then the commit was a no-op; nothing needed
* to be committed.
*
* {@link #run()} returns {@link SVNCommitInfo} information about new committed revision.
* 
* {@link #run()} throws {@link org.tmatesoft.svn.core.SVNException} in the following cases:
*             <ul>
*             <li/>exception with {@link SVNErrorCode#UNSUPPORTED_FEATURE} error code 
*             - if it is commit from different working copies belonging to different repositories
*             <li/>exception with {@link SVNErrorCode#CLIENT_PROPERTY_NAME} error code 
*             - if there is standard Subversion property among revision properties
*             <li/>exception with {@link SVNErrorCode#WC_FOUND_CONFLICT} error code 
*             - if item is remaining in conflict
*             <li/>exception with {@link SVNErrorCode#ILLEGAL_TARGET} error code 
*             - if item is not under version control
*             or item's parent is not known to exist in the repository and is not part of the commit, yet item is part of the commit
*             <li/>exception with {@link SVNErrorCode#WC_PATH_NOT_FOUND} error code 
*             - if item is scheduled for addition within unversioned parent
*             or item is scheduled for addition, but is missing
*             <li/>exception with {@link SVNErrorCode#NODE_UNKNOWN_KIND} error code 
*             - if item is of unknown kind
*             <li/>exception with {@link SVNErrorCode#NODE_UNEXPECTED_KIND} error code 
*             - item has unexpectedly changed special status
*             <li/>exception with {@link SVNErrorCode#WC_NOT_LOCKED} error code 
*             - if working copy directory/file is missing
*             <li/>exception with {@link SVNErrorCode#CLIENT_DUPLICATE_COMMIT_URL} error code 
*             - if operation trying to commit different items referring to the same URL
*             <li/>exception with {@link SVNErrorCode#BAD_URL} error code 
*             - if working copy directory/file is missing
*             <li/>exception with {@link SVNErrorCode#WC_NOT_LOCKED} error code 
*             - if operation cannot compute base URL for commit operation
*             <li/>exception with {@link SVNErrorCode#WC_CORRUPT_TEXT_BASE} error code 
*             - if working copy is corrupted
*             </ul>
* 
* @author TMate Software Ltd.
* @version 1.7
*/
public class SvnCommit extends AbstractSvnCommit {
    
    private boolean keepChangelists;
    private boolean keepLocks;
    private ISvnCommitParameters commitParameters;
    
    private SvnCommitPacket packet;
    private boolean force;
    private boolean isFailOnMultipleRepositories;
    private boolean combinePackets;
    private SvnCommitPacket[] splitPackets;

    protected SvnCommit(SvnOperationFactory factory) {
        super(factory);
    }
    
    /**
     * Gets whether or not <code>changelists</code> should be removed.
     * 
     * @return <code>true</code> if <code>changelists</code> should be removed, otherwise <code>false</code>
     */
    public boolean isKeepChangelists() {
        return keepChangelists;
    }
    
    /**
     * Sets whether or not <code>changelists</code> should be removed.
     * 
     * @param keepChangelists <code>true</code> if <code>changelists</code> should be removed, otherwise <code>false</code>
     */
    public void setKeepChangelists(boolean keepChangelists) {
        this.keepChangelists = keepChangelists;
    }
     
    /**
     * Gets whether or not to unlock files in the repository.
     * 
     * @return <code>true</code> if files should not be unlocked in the repository, otherwise <code>false</code>
     */
    public boolean isKeepLocks() {
        return keepLocks;
    }

    /**
     * Sets whether or not to unlock files in the repository.
     * 
     * @param keepLocks <code>true</code> if files should not be unlocked in the repository, otherwise <code>false</code>
     */
    public void setKeepLocks(boolean keepLocks) {
        this.keepLocks = keepLocks;
    }

    /**
     * Gets operation's parameters of the commit.
     * 
     * @return commit parameters of the operation
     * @see ISvnCommitParameters 
     */
    public ISvnCommitParameters getCommitParameters() {
        return commitParameters;
    }

    /**
     * Sets operation's parameters of the commit. 
     * 
     * @param commitParameters commit parameters of the operation
     * @see ISvnCommitParameters 
     */
    public void setCommitParameters(ISvnCommitParameters commitParameters) {
        this.commitParameters = commitParameters;
    }
    
    /**
     * Returns operation's commit packet.
     * Checks arguments and calls {@link SvnOperationFactory#collectCommitItems(SvnCommit)} 
     * if commit packet is <code>null</code>.
     * 
     * @return commit packet of the operation
     */
    public SvnCommitPacket collectCommitItems() throws SVNException {
        ensureArgumentsAreValid();        
        if (packet != null) {
            return packet;
        }
        packet = getOperationFactory().collectCommitItems(this);
        return packet;
    }
    
    public SvnCommitPacket[] splitCommitPackets(boolean combinePackets) throws SVNException {
        if (splitPackets != null) {
            return splitPackets;
        }
        splitPackets = collectCommitItems().split(combinePackets);
        return splitPackets;
    }
    
    /**
     * If commit packet is <code>null</code>, calls {@link #collectCommitItems()}
     * to create the commit packet, then executes the operation.  
     */
    public SVNCommitInfo run() throws SVNException {
        if (packet == null) {
            packet = collectCommitItems();
        }
        return super.run();
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        super.ensureArgumentsAreValid();
        if (getDepth() == SVNDepth.UNKNOWN) {
            setDepth(SVNDepth.INFINITY);
        }
    }

    @Override
    protected int getMaximumTargetsCount() {
        return Integer.MAX_VALUE;
    }

    /**
     * Gets whether or not to force a non-recursive commit; if <code>depth</code> 
     * is {@link SVNDepth#INFINITY} the <code>force</code> flag is ignored.
     * 
     * @param force <code>true</code> if non-recursive commit should be forced, otherwise <code>false</code>
     */
    public void setForce(boolean force) {
        this.force = force;
    }
    
    /**
     * Sets whether or not to force a non-recursive commit; if <code>depth</code> 
     * is {@link SVNDepth#INFINITY} the <code>force</code> flag is ignored.
     * 
     * @return <code>true</code> if non-recursive commit should be forced, otherwise <code>false</code>
     */
    public boolean isForce() {
        return this.force;
    }

    /**
     * Gets whether the operation changes working copy
     * @return <code>true</code> if the operation changes the working copy, otherwise <code>false</code>
     */
    @Override
    public boolean isChangesWorkingCopy() {
        return true;
    }
    
    public void setCombinePackets(boolean combine) {
        this.combinePackets = combine;
    }
    
    public boolean isCombinePackets() {
        return this.combinePackets;
    }

    public boolean isFailOnMultipleRepositories() {
        return this.isFailOnMultipleRepositories;
    }
    
    public void setFailOnMultipleRepositories(boolean fail) {
        this.isFailOnMultipleRepositories = fail;
    }

    @Override
    protected void initDefaults() {
        super.initDefaults();
        setCombinePackets(true);
    }
    
    
}
