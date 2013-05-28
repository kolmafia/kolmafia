package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNErrorMessage;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.internal.wc.SVNErrorManager;
import org.tmatesoft.svn.util.SVNLogType;

/**
 * Represents set changelist operation.
 * 
 * Adds/remove each path in <code>targets</code> (recursing to <code>depth</code> as
 * necessary) to <code>changelist</code>. If a path is already a member of
 * another changelist, then removes it from the other changelist and adds it
 * to <code>changelist</code>. (For now, a path cannot belong to two
 * changelists at once.)
 * 
 * <p/>
 * <code>changelists</code> is an array of <code>String</code> changelist
 * names, used as a restrictive filter on items whose changelist assignments
 * are adjusted; that is, doesn't tweak the change set of any item unless
 * it's currently a member of one of those changelists. If
 * <code>changelists</code> is empty (or <code>null</code>), 
 * no changelist filtering occurs.
 * 
 * <p/>
 * Note: this metadata is purely a client-side "bookkeeping" convenience,
 * and is entirely managed by the working copy.
 * 
 * <p/>
 * Note: this method does not require repository access.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnSetChangelist extends SvnOperation<Void> {

    private String changelistName;
    private boolean remove;
    
    protected SvnSetChangelist(SvnOperationFactory factory) {
        super(factory);
    }
    
    /**
     * Returns the operation's changelist name.
     * 
     * @return changelist name of the operation
     */
    public String getChangelistName() {
        return changelistName;
    }

    /**
     * Sets the operation's changelist name.
     * 
     * @param changelistName changelist name of the operation
     */
    public void setChangelistName(String changelistName) {
        this.changelistName = changelistName;
    }
    
    /**
     * Returns whether <code>targets</code> should be removed from changelist.
     * 
     * @return <code>true</code> if <code>targets</code> should be removed from changelist, if <code>false</code> should be added
     */
    public boolean isRemove() {
        return remove;
    }

    /**
     * Sets whether <code>targets</code> should be removed from changelist.
     * 
     * @param remove <code>true</code> if <code>targets</code> should be removed from changelist, if <code>false</code> should be added
     */
    public void setRemove(boolean remove) {
        this.remove = remove;
    }
    
    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        super.ensureArgumentsAreValid();
        
        if (!isRemove()) {
        	if ("".equals(getChangelistName())) {
        		SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.BAD_CHANGELIST_NAME, "Target changelist name must not be empty");
        		SVNErrorManager.error(err, SVNLogType.WC);
        	}
        }
        
        if (hasRemoteTargets()) {
            SVNErrorMessage err = SVNErrorMessage.create(SVNErrorCode.ILLEGAL_TARGET, "''{0}'' is not a local path", getFirstTarget().getURL());
            SVNErrorManager.error(err, SVNLogType.WC);
        }
        
        
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
        return true;
    }
}
