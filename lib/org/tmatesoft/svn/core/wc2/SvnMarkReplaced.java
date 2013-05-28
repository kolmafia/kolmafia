package org.tmatesoft.svn.core.wc2;

/**
 * Schedules <code>target</code> as being replaced. This method does not
 * perform any deletion\addition in the filesystem nor does it require a
 * connection to the repository. It just marks the current <code>target</code>
 * item as being replaced.
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnMarkReplaced extends SvnOperation<Void> {

    protected SvnMarkReplaced(SvnOperationFactory factory) {
        super(factory);
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
