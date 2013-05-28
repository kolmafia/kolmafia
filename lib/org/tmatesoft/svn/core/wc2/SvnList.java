package org.tmatesoft.svn.core.wc2;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNErrorCode;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.wc.SVNRevision;

/**
 * Represents list operation.
 * Reports the directory entry, and possibly children, for <code>target</code>
 * at <code>revision</code>. The actual node revision selected is determined
 * by the <code>target</code>'s path as it exists in its <code>pegRevision</code>. If
 * <code>target</code>'s <code>pegRevision</code> is {@link SVNRevision#isValid() invalid}, then
 * it defaults to {@link SVNRevision#HEAD}.
 * 
 * <p/>
 * If <code>depth</code> is {@link SVNDepth#EMPTY}, lists just
 * <code>target</code> itself. If <code>depth</code> is {@link SVNDepth#FILES},
 * lists <code>target</code> and its file entries. If
 * {@link SVNDepth#IMMEDIATES}, lists its immediate file and directory
 * entries. If {@link SVNDepth#INFINITY}, lists file entries and recurses
 * (with {@link SVNDepth#INFINITY}) on directory entries.
 * 
 * <p/>
 * Note: this routine requires repository access.
 * 
 * <p/>
 * {@link #run()} method returns an array of <code>SVNDirEntry</code> objects.
 * It throws {@link SVNException} in the following cases:
 *             <ul>
 *             <li/>exception with {@link SVNErrorCode#FS_NOT_FOUND} error
 *             code - if <code>url</code> is non-existent in the repository
 *             <ul/>
 *             
 * @author TMate Software Ltd.
 * @version 1.7
 */
public class SvnList extends SvnReceivingOperation<SVNDirEntry> {

    private boolean isFetchLocks;
    private int entryFields;
    
    protected SvnList(SvnOperationFactory factory) {
        super(factory);
        setEntryFields(SVNDirEntry.DIRENT_ALL);
    }

   /**
    * Returns entry fields whose controls which fields in the {@link SVNDirEntry}
    * are filled in. To have them totally filled in use 
    * {@link SVNDirEntry#DIRENT_ALL}, otherwise simply bitwise OR together the
    * 	combination of fields you care about.
    *
    * @return entry fields flags 
    */
    public int getEntryFields() {
        return entryFields;
    }

    /**
     * Sets entry fields whose controls which fields in the {@link SVNDirEntry}
     * are filled in. To have them totally filled in use 
     * {@link SVNDirEntry#DIRENT_ALL}, otherwise simply bitwise OR together the
     * 	combination of fields you care about.
     *
     * @param entryFields entry fields flags 
     */
    public void setEntryFields(int entryFields) {
        this.entryFields = entryFields;
    }

    /**
     * Returns whether to fetch locks information
     *  
     * @return <code>true</code> if the lock information should be fetched, otherwise <code>false</code>
     */
    public boolean isFetchLocks() {
        return isFetchLocks;
    }

    /**
     * Sets whether to fetch locks information
     *  
     * @param isFetchLocks <code>true</code> if the lock information should be fetched, otherwise <code>false</code>
     */
    public void setFetchLocks(boolean isFetchLocks) {
        this.isFetchLocks = isFetchLocks;
    }

    /**
     * Gets whether the operation changes working copy
     * @return <code>true</code> if the operation changes the working copy, otherwise <code>false</code>
     */
    @Override
    public boolean isChangesWorkingCopy() {
        return false;
    }

    @Override
    protected void initDefaults() {
        super.initDefaults();
        setDepth(SVNDepth.IMMEDIATES);
    }
}
