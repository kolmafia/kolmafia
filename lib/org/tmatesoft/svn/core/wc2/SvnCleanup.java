package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNErrorCode;

/**
 * Represents cleanup operation. Recursively cleans up the working copy, removing locks and resuming
 * unfinished operations.
 * 
 * <code>Target</code> should represent working copy path.
 * 
 * <p/>
 * If you ever get a "working copy locked" error, use this method to remove
 * stale locks and get your working copy into a usable state again.
 * 
 * <p>
 * This method operates only on working copies and does not open any network
 * connection.
 * 
 * <p/>
 * {@link #run()} method throws  {@link org.tmatesoft.svn.core.SVNException} if one of the following is true:
 * <ul>
 * <li>exception with {@link SVNErrorCode#ILLEGAL_TARGET} error code 
 * - if <code>target</code> is URL
 * <li>exception with {@link SVNErrorCode#WC_NOT_WORKING_COPY} error code 
 * - if <code>target</code> is not under version control
 * </ul>
 *             
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnCleanup extends SvnOperation<Void> {
	
	private boolean deleteWCProperties;
    private boolean vacuumPristines;
    private boolean removeUnversionedItems;
    private boolean removeIgnoredItems;
    private boolean includeExternals;
    private boolean breakLocks;

    protected SvnCleanup(SvnOperationFactory factory) {
        super(factory);
    }
    
    /**
    * Gets whether or not DAV specific <code>"svn:wc:"</code> properties
    * should be removed from the working copy
    * 
    * @return <code>true</code> if properties will be removed, otherwise <code>false</code>
    */
    public boolean isDeleteWCProperties() {
        return deleteWCProperties;
    }

    /**
     * Sets whether or not DAV specific <code">"svn:wc:"</code> properties
     * should be removed from the working copy
     * 
     * @param deleteWCProperties <code>true</code> if properties will be removed, otherwise <code>false</code>
     */
    public void setDeleteWCProperties(boolean deleteWCProperties) {
        this.deleteWCProperties = deleteWCProperties;
    }

    public boolean isVacuumPristines() {
        return vacuumPristines;
    }

    public void setVacuumPristines(boolean vacuumPristines) {
        this.vacuumPristines = vacuumPristines;
    }

    public boolean isRemoveUnversionedItems() {
        return removeUnversionedItems;
    }

    public void setRemoveUnversionedItems(boolean removeUnversionedItems) {
        this.removeUnversionedItems = removeUnversionedItems;
    }

    public boolean isRemoveIgnoredItems() {
        return removeIgnoredItems;
    }

    public void setRemoveIgnoredItems(boolean removeIgnoredItems) {
        this.removeIgnoredItems = removeIgnoredItems;
    }

    public boolean isIncludeExternals() {
        return includeExternals;
    }

    public void setIncludeExternals(boolean includeExternals) {
        this.includeExternals = includeExternals;
    }

    public void setBreakLocks(boolean breakLocks) {
        this.breakLocks = breakLocks;
    }

    public boolean isBreakLocks() {
        return breakLocks;
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
