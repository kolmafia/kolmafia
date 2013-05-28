package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNLock;

/**
 * Represents operation for unlocking files. 
 * Unlocks file items in a working copy as well as in a repository.
 * 
 * <p/>
 * {@link #run()} method returns {@link SVNLock} object that represents information of lock.
 * {@link #run()} method @throws {@link org.tmatesoft.svn.core.SVNException} if one of the following is true:
 *             <ul>
 *             <li>a <code>target</code>'s path is not under version control
 *             <li>can not obtain a URL of a local <code>target</code>'s path to unlock it in the
 *             repository - there's no such entry
 *             <li>if a path is not locked in the working copy and
 *             <code>breakLock</code> is <code>false</code>
 *             <li><code>targets</code> to be unlocked belong to different
 *             repositories
 *             </ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnUnlock extends SvnReceivingOperation<SVNLock> {

	private boolean breakLock;
	
    protected SvnUnlock(SvnOperationFactory factory) {
        super(factory);
    }
    
   /**
   * Gets whether or not the locks belonging to different users should be also unlocked ("broken")
   * 
   * @return <code>true</code> if other users locks should be "broken", otherwise <code>false</code>
   */
    public boolean isBreakLock() {
        return breakLock;
    }
    
    /**
     * Sets whether or not the locks belonging to different users should be also unlocked ("broken")
     * 
     * @param breakLock <code>true</code> if other users locks should be "broken", otherwise <code>false</code>
     */
    public void setBreakLock(boolean breakLock) {
        this.breakLock = breakLock;
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
