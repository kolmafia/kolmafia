package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNException;



/**
 * Represents remove operation.
 * Schedules the working copy <code>targets</code> for deletion.
 * 
 * <p/>
 * This operation allows to choose
 * whether file item(s) are to be deleted from the filesystem or not, it is controlled by <code>deleteFiles</code>.
 * 
 * <p/>
 * This method deletes only local working copy paths without connecting to
 * the repository.
 * 
 * <p/>
 * <code>Targets</code> that are, or contain, unversioned or modified items will
 * not be removed unless the <code>force</code> and <code>deleteFiles</code> is <code>true</code>.
 *  
 * 
 * @param deleteFiles
 *            if <code>true</code> then files will be
 *            scheduled for deletion as well as deleted from the filesystem,
 *            otherwise files will be only scheduled for addition and still
 *            be present in the filesystem
 * @param dryRun
 *            <code>true</code> only to try the delete
 *            operation without actual deleting
 * 
 * <p/>
 * {@link #run()} throws {@link SVNException} if one of the following is true:
 *             <ul>
 *             <li><code>target</code>'s path is not under version control 
 *             <li>can not delete <code>target</code>'s path without forcing
 *             </ul>
 *             
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnScheduleForRemoval extends SvnOperation<Void> {
    
    private boolean force;
    private boolean dryRun;
    private boolean deleteFiles;

    protected SvnScheduleForRemoval(SvnOperationFactory factory) {
        super(factory);
    }

    /**
     * Returns whether to force operation on unversioned or modified items.
     * 
     * @return <code>true</code> if the operation should be forced on unversioned or modified items
     */
    public boolean isForce() {
        return force;
    }

    /**
     * Sets whether to force operation on unversioned or modified items.
     * 
     * @param force <code>true</code> if the operation should be forced on unversioned or modified items
     */
    public void setForce(boolean force) {
        this.force = force;
    }

    /**
     * Returns whether to check the possibility of delete operation without actual deleting
     * 
     * @return <code>true</code> the possibility of delete operation should be checked without actual deleting, otherwise false
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * Sets whether to check the possibility of delete operation without actual deleting
     * 
     * @param dryRun <code>true</code> the possibility of delete operation should be checked without actual deleting, otherwise false
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * Sets whether files should be scheduled for deletion as well as deleted from the filesystem,
     * or files should be only scheduled for addition and still be present in the filesystem.
     * 
     * @param deleteFiles <code>true</code> if files should be deleted on filesystem, otherwise <code>false</code>.
     */
    public void setDeleteFiles(boolean deleteFiles) {
        this.deleteFiles = deleteFiles;
    }

    /**
     * Returns whether files should be scheduled for deletion as well as deleted from the filesystem,
     * or files should be only scheduled for addition and still be present in the filesystem.
     * 
     * @return <code>true</code> if files should be deleted on filesystem, otherwise <code>false</code>.
     */
    public boolean isDeleteFiles() {
        return deleteFiles;
    }

    @Override
    protected void initDefaults() {
        super.initDefaults();
        setDeleteFiles(true);
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
