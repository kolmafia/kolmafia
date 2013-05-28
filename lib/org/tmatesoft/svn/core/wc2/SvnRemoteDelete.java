package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNCommitInfo;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;

/**
 * Represents delete operation. Deletes items from a repository.
 * 
 * <p/>
 * All <code>targets</code> should be URLs, representing repository locations to be removed. 
 * URLs can be from multiple repositories.
 * 
 * <p/>
 * <code>commitHandler</code> will be asked for a commit log message.
 * 
 * <p/>
 * If the caller's {@link ISVNEventHandler event handler} is not <code>null</code> and if the commit succeeds, the handler
 * will be called with {@link SVNEventAction#COMMIT_COMPLETED} event action.
 *
 * <p/>
 * {@link #run()} method returns {@link SVNCommitInfo} information on a new revision as the result of the commit.
 *  
 * {@link #run()} throws {@link org.tmatesoft.svn.core.SVNException} in the following cases:
 *			   <ul>
 *             <li/>exception with {@link SVNErrorCode#RA_ILLEGAL_URL} error code 
 *             - if cannot compute common root url for <code>targets</code>, 
 *             <code>targets</code> can can refer to different repositories
 *             <li/>exception with {@link SVNErrorCode#CLIENT_PROPERTY_NAME} error code 
 *             - if there is standard Subversion property among revision properties
 *             <li/>exception with {@link SVNErrorCode#FS_NOT_FOUND} error code 
 *             - if some of the <code>targets</code> does not exist
 *             <li/>exception with {@link SVNErrorCode#RA_ILLEGAL_URL} error code 
 *             - if some of the <code>targets</code> is not within a repository
 *             </ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnRemoteDelete extends AbstractSvnCommit {

    protected SvnRemoteDelete(SvnOperationFactory factory) {
        super(factory);
    }

    @Override
    protected int getMaximumTargetsCount() {
        return Integer.MAX_VALUE;
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
