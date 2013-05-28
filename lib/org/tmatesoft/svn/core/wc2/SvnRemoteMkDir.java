package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.wc.ISVNEventHandler;
import org.tmatesoft.svn.core.wc.SVNEventAction;

/**
 * Creates directory(ies) in a repository. 
 * 
 * <p/>
 * All <code>targets</code> should be URLs, representing repository locations to be created. 
 * URLs can be from multiple repositories.
 * 
 * <p/>
 * If non-<code>null</code>, <code>revisionProperties</code> holds additional, custom revision
 * properties (<code>String</code> names mapped to {@link SVNPropertyValue}
 * values) to be set on the new revision. This table cannot contain any
 * standard Subversion properties.
 * 
 * <p/>
 * <code>commitHandler</code> will be asked for a commit log message.
 * 
 * <p/>
 * If the caller's {@link ISVNEventHandler event handler} is not <code>null</code> and if the commit succeeds, the handler
 * will be called with {@link SVNEventAction#COMMIT_COMPLETED} event action.
 * 
 * <p/>
 * {@link #run()} method returns {@link org.tmatesoft.svn.core.SVNCommitInfo} information on a new revision as the result of the commit.
 * 
 * {@link #run()} throws {@link org.tmatesoft.svn.core.SVNException} in the following cases:
 *             <ul>
 *             <li/>exception with {@link SVNErrorCode#RA_ILLEGAL_URL} error code 
 *             - if cannot compute common root url for <code>targets</code>, 
 *             <code>targets</code> can refer to different repositories
 *             <li/>exception with {@link SVNErrorCode#CLIENT_PROPERTY_NAME} error code 
 *             - if there is standard Subversion property among revision properties
 *             <li/>exception with {@link SVNErrorCode#FS_NOT_FOUND} error code 
 *             - if some of the <code>targets</code> does not exist
 *             </ul>
 * 
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnRemoteMkDir extends AbstractSvnCommit {

    private boolean makeParents;
    
    /**
     * Returns whether to create all non-existent parent directories
     * @return <code>true</code> if the non-existent parent directories should be created, otherwise <code>false</code>
     */
    public boolean isMakeParents() {
        return makeParents;
    }

    /**
     * Sets whether to create all non-existent parent directories
     * @param makeParents <code>true</code> if the non-existent parent directories should be created, otherwise <code>false</code>
     */
    public void setMakeParents(boolean makeParents) {
        this.makeParents = makeParents;
    }

    protected SvnRemoteMkDir(SvnOperationFactory factory) {
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
