package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNLock;

/**
 * Represents operation for locking files. 
 * Locks file items in a working copy as well as in a repository so that 
 * no other user can commit changes to them.
 * 
 * <p/>
 * {@link #run()} method returns {@link SVNLock} object that represents information of lock.
 * {@link #run()} method throws {@link org.tmatesoft.svn.core.SVNException} if one of the following is true:
 *             <ul>
 *             <li>a <code>target</code>'s path to be locked is not under version control
 *             <li>can not obtain a URL of a local <code>target</code>'s path to lock it in the
 *             repository - there's no such entry
 *             <li><code>targets</code> to be locked belong to different
 *             repositories ((for SVN 1.6 working copy only)
 *             </ul>
 * 
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnSetLock extends SvnReceivingOperation<SVNLock> {

	private boolean stealLock;
	private String lockMessage;
	 
    protected SvnSetLock(SvnOperationFactory factory) {
        super(factory);
    }
    
    /**
     * Gets whether or not all existing locks on the specified targets 
     * will be "stolen" from another user or working copy.
     * 
     * @return <code>true</code> if locks should be "stolen", otherwise <code>false</code>
     */
    public boolean isStealLock() {
        return stealLock;
    }
    
    /**
     * Sets whether or not all existing locks on the specified targets 
     * will be "stolen" from another user or working copy.
     * 
     * @param stealLock <code>true</code> if locks should be "stolen", otherwise <code>false</code>
     */
    public void setStealLock(boolean stealLock) {
        this.stealLock = stealLock;
    }
    
    /**
     * Gets the optional comment for the lock.
     * 
     * @return comment for the lock
     */
    public String getLockMessage() {
        return lockMessage;
    }
    
    /**
     * Sets the optional comment for the lock.
     * 
     * @param lockMessage comment for the lock
     */
    public void setLockMessage(String lockMessage) {
    	this.lockMessage = lockMessage;
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
