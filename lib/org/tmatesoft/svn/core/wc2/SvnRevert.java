package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;

/**
 * Represents revert operation.
 * Restores the pristine version of working copy <code>targets</code>,
 * effectively undoing any local mods. For each path in <code>targets</code>,
 * reverts it if it is a file. Else if it is a directory, reverts according
 * to <code>depth</code>:
 * 
 * <p/>
 * If </code>depth</code> is {@link SVNDepth#EMPTY}, reverts just the
 * properties on the directory; else if {@link SVNDepth#FILES}, reverts the
 * properties and any files immediately under the directory; else if
 * {@link SVNDepth#IMMEDIATES}, reverts all of the preceding plus properties
 * on immediate subdirectories; else if {@link SVNDepth#INFINITY}, reverts
 * path and everything under it fully recursively.
 * 
 * <p/>
 * <code>changeLists</code> is a collection of <code>String</code>
 * changelist names, used as a restrictive filter on items reverted; that
 * is, doesn't revert any item unless it's a member of one of those
 * changelists. If <code>changeLists</code> is empty (or <code>null</code>), 
 * no changelist filtering occurs.
 * 
 * <p/>
 * If an item specified for reversion is not under version control, then
 * does not fail with an exception, just invokes {@link org.tmatesoft.svn.core.wc.ISVNEventHandler}
 * using notification code {@link org.tmatesoft.svn.core.wc.SVNEventAction#SKIP}.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnRevert extends SvnOperation<Void> {

    private boolean revertMissingDirectories;
    private boolean preserveModifiedCopies;

    protected SvnRevert(SvnOperationFactory factory) {
        super(factory);
        preserveModifiedCopies = false; // default Subversion behaviour, see issue #3101 of SVN issue tracker
    }

    public boolean isRevertMissingDirectories() {
        return revertMissingDirectories;
    }

    public void setRevertMissingDirectories(boolean revertMissingDirectories) {
        this.revertMissingDirectories = revertMissingDirectories;
    }

    public boolean isPreserveModifiedCopies() {
        return preserveModifiedCopies;
    }

    public void setPreserveModifiedCopies(boolean preserveModifiedCopies) {
        this.preserveModifiedCopies = preserveModifiedCopies;
    }

    @Override
    protected void ensureArgumentsAreValid() throws SVNException {
        super.ensureArgumentsAreValid();
        if (getDepth() == SVNDepth.UNKNOWN) {
            setDepth(SVNDepth.EMPTY);
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
